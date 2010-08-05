package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Information about a remote peer.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class PeerInfo {
    private static int sequenceCounter = 0;

    private boolean isSeeder = false;
    private boolean sentPiecesMessage = false;
    private boolean deleted = false;
    private CreditValue credit = new CreditValue(0, -1);
    final int sequenceNumber;

    /**
     * Which pieces are we requesting of this peer.
     */
    private final HashSet<Integer> requestedPieces = new HashSet<Integer>();

    /** The set of pieces this peer has. */
    private PieceSet knownPieces;

    /** The upload performance monitor of this peer. */
    private final TransferPerformanceMonitor receiveTransferMonitor = new WindowTransferPerformanceMonitor(
            Settings.TRANSFER_MONITOR_WINDOW_TIME);

    private final TransferPerformanceMonitor sendTransferMonitor = new WindowTransferPerformanceMonitor(
            Settings.TRANSFER_MONITOR_WINDOW_TIME);

    /** The Ibis identifier of this peer. */
    final IbisIdentifier peer;

    /** The list of chunks this peer wants to receive. */
    private final LinkedList<ChunkRequest> chunkRequests = new LinkedList<ChunkRequest>();

    /** How many pieces this peer sent to us. */
    private int transferedPieceCount = 0;

    /** How many pieces we canceled on this peer. */
    private int canceledPieceCount = 0;

    /** How many bytes we have received from this peer. */
    private long totalReceivedBytes = 0L;

    /** How many bytes we have sent to this peer. */
    private long totalSentBytes = 0L;
    private boolean peerHasChokedUs = false;
    private boolean weChokedPeer = false;
    private boolean peerIsInterested = true;
    private boolean weAreInterested = true;
    private boolean peerWantsToTalkToUs = true;
    private final TimeStatistics fulfillmentStatistics = new TimeStatistics();

    /**
     * Constructs a new peer information class for the given peer.
     * 
     * @param peer
     *            The peer for which we are storing information.
     * 
     */
    PeerInfo(final IbisIdentifier peer, final int numberOfPieces) {
        knownPieces = new PieceSet(numberOfPieces);
        this.peer = peer;
        this.sequenceNumber = sequenceCounter++;
    }

    void setKnownPieces(final PieceSet bits) {
        knownPieces = bits;
        isSeeder = bits.isComplete();
        if (Settings.TracePeers) {
            Globals.log.reportProgress("Peer " + peer + ": isSeeder="
                    + isSeeder + " known pieces "
                    + bits.compactBitSetToString());
        }
    }

    /**
     * 
     * @param piece
     *            The piece that this peer is now known to have.
     * @return <code>true</code> iff this peer has newly become a seed.
     */
    boolean registerHasPiece(final int piece) {
        if (isSeeder) {
            // This peer already is a seed, ignore this update.
            return false;
        }
        knownPieces.set(piece);
        isSeeder = knownPieces.isComplete();
        if (Settings.TracePieceCount) {
            Globals.log.reportProgress("Peer " + peer + ": isSeeder="
                    + isSeeder + " " + knownPieces.cardinality()
                    + " known pieces");
        }
        return isSeeder;
    }

    void registerReceivedChunk(final long length) {
        totalReceivedBytes += length;
        receiveTransferMonitor.registerTransfer(length);
    }

    double getReceiveTransferRate() {
        return receiveTransferMonitor.estimatePerformance();
    }

    double getSendTransferRate() {
        return sendTransferMonitor.estimatePerformance();
    }

    void registerPieceDownload(final int piece) {
        requestedPieces.add(piece);
    }

    boolean hasRoomForRequest() {
        return !deleted && !peerHasChokedUs && peerWantsToTalkToUs
                && requestedPieces.size() < Settings.PIECE_REQUESTS_PER_PEER;
    }

    void registerCompletedPiece(final int piece) {
        transferedPieceCount++;
        final boolean removed = requestedPieces.remove(piece);
        if (!removed) {
            Globals.log.reportInternalError("Piece " + piece
                    + " was not a request to peer " + peer
                    + " but it completed it anyway??");
        }
    }

    void registerCanceledPiece(final int piece) {
        canceledPieceCount++;
        final boolean removed = requestedPieces.remove(piece);
        if (!removed) {
            Globals.log.reportInternalError("Piece " + piece
                    + " was not a request to peer " + peer
                    + " but it canceled it anyway??");
        }
    }

    void addChunkRequest(final Chunk chunk) {
        chunkRequests.add(new ChunkRequest(chunk, peer));
        if (Settings.TraceChunkRequests) {
            Globals.log.reportProgress("Registered request for chunk " + chunk
                    + " from peer " + peer);
        }
    }

    ChunkRequest getChunkRequest() {
        if (deleted) {
            return null;
        }
        final ChunkRequest c = chunkRequests.poll();
        if (c == null) {
            return null;
        }
        totalSentBytes += c.chunk.size;
        sendTransferMonitor.registerTransfer(c.chunk.size);
        final long duration = System.currentTimeMillis() - c.requestMoment;
        fulfillmentStatistics.registerSample(duration * 1e-3);
        if (Settings.TraceChunkRequests) {
            Globals.log.reportProgress("Dequeued ChunkRequest " + c);
        }
        return c;
    }

    boolean removeChunkRequest(final Chunk chunk) {
        int ix = chunkRequests.size();
        boolean progress = false;
        while (ix > 0) {
            ix--;
            final ChunkRequest e = chunkRequests.get(ix);
            if (e.chunk.equals(chunk)) {
                chunkRequests.remove(ix);
                if (Settings.TraceChunkRequests) {
                    Globals.log.reportProgress("Removed ChunkRequest " + e);
                }
                progress = true;
            }
        }
        return progress;
    }

    PieceSet getPotentialPieces(final PieceSetViewer peerMissingPieces) {
        final PieceSet res = knownPieces.clone();

        res.and(peerMissingPieces);
        if (Settings.TracePiecePicking) {
            Globals.log.reportProgress("getPotentialPieces: we are missing "
                    + peerMissingPieces.cardinality() + " pieces; peer " + peer
                    + " has " + knownPieces.cardinality()
                    + " known pieces, and " + res.cardinality()
                    + " potential pieces for us");
        }
        return res;
    }

    /**
     * Given the set of pieces that are missing on this site, return true iff
     * this peer has any of the missing pieces.
     * 
     * @param missingPieces
     *            The set of pieces we need on this peer.
     * 
     * @return <code>true</code> iff this peer has any of the missing pieces.
     */
    boolean hasSomeOfOurMissingPieces(final PieceSet missingPieces) {
        if (deleted || peerHasChokedUs) {
            return false;
        }
        if (isSeeder) {
            return true;
        }
        return knownPieces.intersects(missingPieces);
    }

    boolean isSeeder() {
        return isSeeder;
    }

    void printStatistics(final PrintStream s) {
        s.format("%9d %9d %9s %9s %s\n", transferedPieceCount,
                canceledPieceCount, Utils.formatByteCount(totalReceivedBytes),
                Utils.formatByteCount(totalSentBytes), peer.toString());
    }

    int getTransferedPieceCount() {
        return transferedPieceCount;
    }

    void dumpState(final PrintStream s) {
        final long upBps = (long) receiveTransferMonitor.estimatePerformance();
        final long downBps = (long) sendTransferMonitor.estimatePerformance();
        s.format("%9d %9d %9s %9s %9s/s %9s/s %8s\n", transferedPieceCount,
                canceledPieceCount, Utils.formatByteCount(totalReceivedBytes),
                Utils.formatByteCount(totalSentBytes),
                Utils.formatByteCount(upBps), Utils.formatByteCount(downBps),
                peer.toString());
    }

    void dumpPeerState(final PrintStream s) {
        s.println("  " + peer + " " + Utils.toStringClassScalars(this) + " "
                + knownPieces.cardinality() + " known pieces");
    }

    void dumpOutstandingRequests(final PrintStream s) {
        if (!requestedPieces.isEmpty()) {
            s.println("  " + peer + ": requestedPieces=" + requestedPieces);
        }
    }

    void dumpFulfillmentStatistics(final PrintStream s) {
        fulfillmentStatistics.printStatistics(s, " " + peer);
    }

    void dumpChunkRequests(final PrintStream s) {
        if (!chunkRequests.isEmpty()) {
            final Object[] rl = chunkRequests.toArray();
            s.println("  " + peer + ": " + Arrays.deepToString(rl));
        }
    }

    void setPeerHasChokedUs(final boolean flag) {
        if (Settings.TraceChoking) {
            Globals.log.reportProgress("peerHasChokedUs=" + flag + " for "
                    + peer);
        }
        peerHasChokedUs = flag;
    }

    void setPeerIsInterestedInUs(final boolean flag) {
        if (Settings.TraceChoking) {
            Globals.log.reportProgress("peerIsInterested=" + flag + " for "
                    + peer);
        }
        this.peerIsInterested = peerWantsToTalkToUs && flag;
    }

    void setWeChokedPeer(final Transmitter transmitter, final String reason) {
        if (this.weChokedPeer != true) {
            this.weChokedPeer = true;
            transmitter.addToBookkeepingQueue(peer, new ChokedMessage(true,
                    reason));
            if (Settings.TraceChoking) {
                Globals.log.reportProgress(true,
                        "weChokedPeer=true changed=true (" + reason + ") for "
                                + peer);
            }
        }
    }

    void setWeUnchokedPeer(final Transmitter transmitter, final String reason) {
        if (this.weChokedPeer != false) {
            this.weChokedPeer = false;
            transmitter.addToBookkeepingQueue(peer, new ChokedMessage(false,
                    reason));
            if (Settings.TraceChoking) {
                Globals.log.reportProgress(true,
                        "weChokedPeer=false changed=true (" + reason + ") for "
                                + peer);
            }
        }
    }

    void setWeAreInterested(final Transmitter transmitter, final String reason) {
        if (this.weAreInterested != true) {
            this.weAreInterested = true;
            transmitter
                    .addToBookkeepingQueue(peer, new InterestedMessage(true));
            if (Settings.TraceChoking) {
                Globals.log.reportProgress(true,
                        "weAreInterested=true changed=true (" + reason
                                + ") for " + peer);
            }
        }
    }

    void setWeAreUninterested(final Transmitter transmitter, final String reason) {
        if (this.weAreInterested != false) {
            this.weAreInterested = false;
            transmitter.addToBookkeepingQueue(peer,
                    new InterestedMessage(false));
            if (Settings.TraceChoking) {
                Globals.log.reportProgress(true,
                        "weAreInterested=false changed=true (" + reason
                                + ") for " + peer);
            }
        }
    }

    boolean weChokedPeer() {
        return weChokedPeer;
    }

    @Override
    public String toString() {
        return "[peer=" + peer + "]";
    }

    boolean weAreInterested() {
        return weAreInterested;
    }

    void setSentPiecesMessage() {
        sentPiecesMessage = true;
    }

    boolean haveSentPiecesMessage() {
        return sentPiecesMessage;
    }

    boolean needsHaveMessage() {
        return !deleted && peerWantsToTalkToUs && sentPiecesMessage;
    }

    boolean needsShutdownMessage() {
        return !deleted && peerWantsToTalkToUs;
    }

    boolean isDeleted() {
        return deleted;
    }

    boolean peerHasChokedUs() {
        return peerHasChokedUs;
    }

    boolean peerIsInterested() {
        return peerIsInterested;
    }

    void setPeerWantsToTalkToUs(final boolean flag) {
        peerWantsToTalkToUs = flag;
        if (!flag) {
            chunkRequests.clear();
            sentPiecesMessage = false;
            peerIsInterested = false;
        }
    }

    void setDeleted() {
        deleted = true;
        isSeeder = false;
        peerIsInterested = false;
        peerWantsToTalkToUs = false;

        // Now free some memory.
        requestedPieces.clear();
        chunkRequests.clear();
    }

    boolean updateCredit(final CreditValue cv) {
        this.credit = this.credit.update(cv);
        return this.credit == cv;
    }

    double getCredit() {
        return credit.value;
    }

    void deregisterPieces(final PieceRanker pieceRanker) {
        pieceRanker.removeOccurrenceCount(knownPieces);
    }

    int getBestPieceToDownload(final PieceRanker pieceRanker) {
        return pieceRanker.getBestPieceToDownload(knownPieces);
    }

    int getBestPieceToDownload(final PieceRanker pieceRanker,
            final OutstandingDownloadingPieceList outstandingPieces) {
        return pieceRanker.getBestPieceToDownload(knownPieces,
                outstandingPieces, peer);
    }

    int getBestPieceToDownload(final PieceRanker pieceRanker,
            final int maximalReplication) {
        return pieceRanker.getBestPieceToDownload(knownPieces,
                maximalReplication);
    }

}
