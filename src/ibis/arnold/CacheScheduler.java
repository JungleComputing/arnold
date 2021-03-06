package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.List;

/**
 * Try to gain credit points by caching popular pieces for a swarm.
 * 
 * @author Kees van Reeuwijk
 * 
 */
final class CacheScheduler implements SchedulerInterface {
    private final EngineInterface engine;
    private final Transmitter transmitter;
    private final PieceSet knownPieces;
    private final PieceSet unrequestedMissingPieces;
    private final PeerSet nonNeighbors = new PeerSet();
    private final PeerSet bannedPeers = new PeerSet();
    private final PeerInfoList neighbors;
    private final OutstandingDownloadingPieceList outstandingPieces = new OutstandingDownloadingPieceList();
    private final PieceRanker pieceRanker;

    CacheScheduler(final EngineInterface engine, final Transmitter transmitter,
            final PieceSet knownPieces, final PieceSet missingPieces,
            final int numberOfPieces) {
        this.engine = engine;
        this.transmitter = transmitter;
        this.knownPieces = knownPieces.clone();
        this.unrequestedMissingPieces = missingPieces;
        neighbors = new PeerInfoList(numberOfPieces);
        this.pieceRanker = new PieceRanker(knownPieces);
    }

    private boolean selectAsNeighbour(final IbisIdentifier peer) {
        final boolean needsCatalogMessage;
        if (neighbors.contains(peer)) {
            needsCatalogMessage = !neighbors.haveSentPiecesMessage(peer);
        } else {
            needsCatalogMessage = true;
            if (Settings.TraceScheduler) {
                Globals.log.reportProgress("CacheScheduler: selected peer "
                        + peer + " as neighbor");
            }
            nonNeighbors.remove(peer);
            neighbors.addPeer(peer, false, transmitter);
        }

        if (needsCatalogMessage) {
            sendCatalogMessage(peer);
        }
        return needsCatalogMessage;
    }

    private void sendCatalogMessage(final IbisIdentifier peer) {
        final BitSetMessage msg = new BitSetMessage(knownPieces);
        if (Settings.TraceScheduler) {
            Globals.log
                    .reportProgress("CacheScheduler: send our bitset to neighbor "
                            + peer);
        }
        transmitter.addToBookkeepingQueue(peer, msg);
        neighbors.setSentCatalog(peer);
    }

    /**
     * Tries to select a new neighbor.
     * 
     * @return <code>true</code> iff we managed to get a new peer.
     */
    private boolean selectANewNeighbor() {

        if (!nonNeighbors.isEmpty()) {
            // If there is any, give new talent a chance.
            final IbisIdentifier peer = nonNeighbors.extractRandomElement();
            selectAsNeighbour(peer);
            return true;
        }
        return neighbors.addAPeer(transmitter, unrequestedMissingPieces);
    }

    private void maintainNeighbors() {
        // Try to maintain a minimum number of neighbors. We may
        // have more active neighbors, but that is based on
        // the idleness of the transmitter.
        while (neighbors.peerSetIsTooSmall(Settings.NEIGHBOR_SET_SIZE)) {
            final boolean progress = selectANewNeighbor();
            if (!progress) {
                // We couldn't make any progress, give up.
                break;
            }
        }
    }

    private void maintainOutstandingPieces() {
        final List<PeerInfo> peersRankedForLeecher = neighbors
                .getRewardRankedPeers();
        final int maximalReplication = (int) (neighbors.size() * Settings.MAXIMAL_CACHE_REPLICATION_FRACTION);
        for (final PeerInfo p : peersRankedForLeecher) {
            if (p.weAreInterested() && p.hasRoomForRequest()) {
                final int piece = p.getBestPieceToDownload(pieceRanker,
                        maximalReplication);
                if (piece >= 0) {
                    createPieceRequest(p, piece);
                } else {
                    p.setWeAreUninterested(transmitter, "No pieces we need");
                }
            }
        }
    }

    private void createPieceRequest(final PeerInfo peer, final int piece) {
        peer.registerPieceDownload(piece);
        final OutstandingDownloadingPiece outstandingPiece = new OutstandingDownloadingPiece(
                peer, piece);
        outstandingPieces.add(outstandingPiece);
        unrequestedMissingPieces.clear(piece);
        engine.startPieceDownload(peer.peer, piece);
        pieceRanker.registerDownloadStart(piece);
        if (Settings.TraceScheduler) {
            Globals.log
                    .reportProgress("CacheScheduler: instructed engine to download piece "
                            + piece + " from " + peer.peer);
        }
    }

    @Override
    public void addPeer(final IbisIdentifier peer,
            final boolean startCommunicating,
            final PersonalityInterface personality) {
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("Scheduler: new peer " + peer);
        }
        personality.newPeer(peer);
        if (startCommunicating) {
            selectAsNeighbour(peer);
        } else {
            nonNeighbors.add(peer);
            maintainNeighbors();
        }
    }

    @Override
    public void addPeer(final PeerInfo peer) {
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("CacheScheduler: new active peer "
                    + peer);
        }
        neighbors.addPeer(peer, false, transmitter);
        if (!peer.haveSentPiecesMessage()) {
            sendCatalogMessage(peer.peer);
        }
    }

    @Override
    public boolean removePeer(final IbisIdentifier peer) {
        boolean res = nonNeighbors.remove(peer);
        final PeerInfo pi = neighbors.removePeer(peer);
        if (pi != null) {
            outstandingPieces.removePeer(pi, unrequestedMissingPieces,
                    pieceRanker);
            pi.deregisterPieces(pieceRanker);
            res = true;
        }
        res |= bannedPeers.remove(peer);
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("CacheScheduler: remove peer " + peer);
        }
        maintainNeighbors(); // Make sure we still have enough neighbours.
        return res;
    }

    @Override
    public PeerInfo extractPeer(final IbisIdentifier peer) {
        final PeerInfo pi = neighbors.extractPeer(peer);
        if (pi != null) {
            outstandingPieces.removePeer(pi, unrequestedMissingPieces,
                    pieceRanker);
            pi.deregisterPieces(pieceRanker);
        }
        maintainNeighbors(); // Make sure we still have enough neighbours.
        return pi;
    }

    @Override
    public void registerPeerHasPiece(final IbisIdentifier source,
            final int piece, final PersonalityInterface personality) {
        final boolean weMayHavePiece = !unrequestedMissingPieces.get(piece);
        neighbors.peerHasPiece(source, piece, weMayHavePiece, false,
                personality, transmitter);
        pieceRanker.addOccurrenceCount(piece);
        maintainNeighbors(); // Make sure we still have enough neighbours.
        maintainOutstandingPieces();
    }

    @Override
    public void setPeerHasPieces(final IbisIdentifier source,
            final PieceSet bits, final PersonalityInterface personality) {
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("Peer " + source + " sent bitset "
                    + bits.compactBitSetToString());
        }
        selectAsNeighbour(source);
        neighbors.setBitSet(source, bits, false, personality, transmitter);
        pieceRanker.addOccurrenceCount(bits);
        maintainNeighbors(); // Make sure we still have enough neighbors.
        maintainOutstandingPieces();
    }

    @Override
    public void requestPieces(final PieceSet set) {
        unrequestedMissingPieces.or(set);
    }

    @Override
    public void registerReceivedChunk(final IbisIdentifier ibis,
            final int length) {
        neighbors.registerReceivedChunk(ibis, length);
    }

    @Override
    public boolean registerIncorrectPiece(final IbisIdentifier peer,
            final int piece) {
        // Remove this peer from our list of neighbors.
        neighbors.removePeer(peer);
        bannedPeers.add(peer);
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("Peer " + peer
                    + " sent an incorrect piece " + piece + "; BANNED");
        }
        neighbors.disconnectPeer(peer, transmitter);
        pieceRanker.removePendingDownload(piece);
        final OutstandingDownloadingPiece p = outstandingPieces.extractPiece(
                peer, piece);
        if (p == null) {
            Globals.log.reportInternalError("No such incorrect piece: piece="
                    + piece + " peer=" + peer);
        }
        if (!outstandingPieces.containsPiece(piece)) {
            // There are no other download attempts for this piece,
            // declare it missing again.
            unrequestedMissingPieces.set(piece);
        }
        maintainNeighbors(); // Make sure we still have enough neighbours.
        maintainOutstandingPieces();
        return true;
    }

    @Override
    public boolean registerFailedPieceDownload(final IbisIdentifier peer,
            final int piece) {
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("Could not download piece " + piece
                    + " from peer " + peer);
        }
        pieceRanker.removePendingDownload(piece);
        final OutstandingDownloadingPiece p = outstandingPieces.extractPiece(
                peer, piece);
        if (p == null) {
            Globals.log.reportInternalError("No such failed piece: piece="
                    + piece + " peer=" + peer);
        }
        if (!outstandingPieces.containsPiece(piece)) {
            // There are no other download attempts for this piece,
            // declare it missing again.
            unrequestedMissingPieces.set(piece);
        }
        maintainNeighbors(); // Make sure we still have enough neighbors.
        maintainOutstandingPieces();
        return false;
    }

    /**
     * Given a piece number, cancel any downloads that are still outstanding for
     * this piece.
     * 
     * 
     * @param piece
     *            The piece number to cancel for.
     */
    private void cancelPieceDownloads(final int piece) {
        int ix = outstandingPieces.size();
        while (ix > 0) {
            ix--;
            final OutstandingDownloadingPiece p = outstandingPieces.get(ix);
            if (p.piece == piece) {
                Globals.log
                        .reportProgress("Canceling duplicate download for piece "
                                + piece + " from peer " + p.peer);
                engine.cancelPieceDownload(p.peer.peer, p.piece);
                neighbors.registerCanceledPiece(p.peer.peer, p.piece);
                outstandingPieces.remove(ix);
            }
        }
    }

    @Override
    public void registerCompletedPiece(final IbisIdentifier peer,
            final int piece) {
        pieceRanker.registerCompletedPiece(piece);
        neighbors.registerCompletedPiece(peer, transmitter, piece);
        final OutstandingDownloadingPiece p = outstandingPieces.extractPiece(
                peer, piece);
        final int canceled = outstandingPieces.cancelPiece(piece, neighbors);
        if (canceled > 0) {
            Globals.log.reportProgress("CacheScheduler: canceled " + canceled
                    + " extra downloads for piece " + piece);
        }
        if (p != null) {
            if (unrequestedMissingPieces.get(piece)) {
                Globals.log.reportInternalError("Completed piece " + piece
                        + " was listed as missing??");
            }
        }
        unrequestedMissingPieces.clear(piece);
        knownPieces.set(piece);
        cancelPieceDownloads(piece);
        if (Settings.TracePieceCount) {
            Globals.log.reportProgress("I now have "
                    + knownPieces.cardinality() + " pieces");
        }
        maintainNeighbors(); // Make sure we still have enough neighbors.
        maintainOutstandingPieces();
    }

    @Override
    public void addChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        neighbors.addChunkRequest(peer, chunk, false, transmitter);
    }

    @Override
    public void removeChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        neighbors.removeChunkRequest(peer, chunk);
    }

    @Override
    public ChunkRequest getNextChunkRequest() {
        return neighbors.getNextChunkRequest();
    }

    @Override
    public boolean haveIncomingChunkRequests() {
        return neighbors.haveIncomingChunkRequests();
    }

    @Override
    public void printStatistics(final PrintStream s) {
        neighbors.printStatistics(s);
    }

    @Override
    public void dumpState() {
        Globals.log.reportProgress(knownPieces.cardinality() + " known, "
                + outstandingPieces.size() + " outstanding, "
                + unrequestedMissingPieces.cardinality() + " missing pieces");
        if (!nonNeighbors.isEmpty()) {
            Globals.log.reportProgress("nonNeighbors=" + nonNeighbors);
        }
        if (!bannedPeers.isEmpty()) {
            Globals.log.reportProgress("bannedPeers=" + bannedPeers);
        }
        Globals.log.reportProgress("outstandingPieces=" + outstandingPieces);
        neighbors.dumpState();
        pieceRanker.dumpState();
        pieceRanker.rankingIsSane();
    }

    @Override
    public void setPeerHasChokedUs(final IbisIdentifier peer, final boolean flag) {
        final boolean isANeighbor = neighbors.setPeerHasChokedUs(peer, flag);
        if (!isANeighbor) {
            Globals.log.reportInternalError("Non-neighbor " + peer
                    + " has choked us");
        }
        maintainNeighbors(); // Make sure we still have enough neighbors.
    }

    @Override
    public void setPeerIsInterested(final IbisIdentifier peer,
            final boolean flag) {
        neighbors.setPeerIsInterested(peer, flag);
        maintainNeighbors(); // Make sure we still have enough neighbors.
    }

    @Override
    public boolean generateMoreTransmission() {
        if (!nonNeighbors.isEmpty()) {
            // If there is any, give new talent a chance.
            final IbisIdentifier peer = nonNeighbors.extractRandomElement();
            return selectAsNeighbour(peer);
        }
        if (!unrequestedMissingPieces.isEmpty()) {
            return neighbors.addAPeer(transmitter, unrequestedMissingPieces);
        }
        return neighbors.addAPeer(transmitter, outstandingPieces
                .getOutstandingPieces(unrequestedMissingPieces.size()));
    }

    @Override
    public void peerHasJoinedAsProxyHelper(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        Globals.log
                .reportInternalError("CacheScheduler: peer sent us a join message: "
                        + peer);
    }

    @Override
    public boolean peerHasResignedAsProxyHelper(final IbisIdentifier peer) {
        Globals.log
                .reportInternalError("CacheScheduler: peer sent us a resignation message: "
                        + peer);
        return false;
    }

    @Override
    public int getSourcePeersCount() {
        return neighbors.size();
    }

    @Override
    public boolean askForHelp(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        // Nope, we're not a helper scheduler.
        final Message msg = new ResignAsHelperMessage();
        transmitter.addToBookkeepingQueue(peer, msg);
        return false;
    }

    @Override
    public void handleStoppedHelping(final IbisIdentifier peer) {
        Globals.log
                .reportProgress("Ignored irrelevant STOP_HELPING message from "
                        + peer);
    }

    @Override
    public void handleClosedConnection(final IbisIdentifier peer) {
        neighbors.setPeerWantsToTalkToUs(peer, false);
    }

    @Override
    public boolean contains(final IbisIdentifier peer) {
        return neighbors.contains(peer) || nonNeighbors.contains(peer)
                || bannedPeers.contains(peer);
    }

    @Override
    public String getName() {
        return "Cache";
    }

    @Override
    public void updateCredit(final IbisIdentifier peer, final CreditValue credit) {
        neighbors.updateCredit(peer, credit);
    }

    @Override
    public void shutdown() {
        neighbors.shutdown(transmitter);
    }

}
