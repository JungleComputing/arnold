package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

/** A scheduler that closely approximates the BitTorrent one. */
final class BitTorrentScheduler implements SchedulerInterface {
    private final EngineInterface engine;
    private final Transmitter transmitter;
    private final PieceSet knownPieces; // Pieces we know about
    private final PieceSet unrequestedMissingPieces;
    private final PeerSet nonNeighbors = new PeerSet();
    private final PeerSet bannedPeers = new PeerSet();
    private final PeerInfoList neighbors;
    private final OutstandingDownloadingPieceList outstandingPieces = new OutstandingDownloadingPieceList();
    private boolean seeding;
    private long lastSlotUpdate = 0L;
    private final PeerInfo slots[] = new PeerInfo[Settings.MAXIMAL_UNCHOKED_PEERS];
    private final PeerRanker peerPerformanceRankerForSeeder;
    private final PeerRanker peerPerformanceRankerForLeecher;
    private final PieceRanker pieceRanker;

    BitTorrentScheduler(final EngineInterface engine,
            final Transmitter transmitter, final PieceSet knownPieces,
            final PieceSet missingPieces, final RankingPolicy policy,
            final int numberOfPieces) {
        this.engine = engine;
        this.transmitter = transmitter;
        this.knownPieces = knownPieces.clone();
        this.unrequestedMissingPieces = missingPieces;
        this.seeding = knownPieces.isComplete();
        peerPerformanceRankerForSeeder = policy
                .getPeerPerformanceRankerForSeeder();
        peerPerformanceRankerForLeecher = policy
                .getPeerPerformanceRankerForLeecher();
        final PeerRanker currentRanker = seeding ? peerPerformanceRankerForSeeder
                : peerPerformanceRankerForLeecher;
        neighbors = new PeerInfoList(numberOfPieces);
        neighbors.setRewardRanker(currentRanker);
        this.pieceRanker = new PieceRanker(knownPieces);
    }

    private void sendCatalogMessage(final IbisIdentifier peer) {
        final BitSetMessage msg = new BitSetMessage(knownPieces);
        if (Settings.TraceScheduler) {
            Globals.log
                    .reportProgress("BitTorrentScheduler: send our bitset to neighbor "
                            + peer);
        }
        transmitter.addToBookkeepingQueue(peer, msg);
        neighbors.setSentCatalog(peer);
    }

    /**
     * Tries to select the given peer as a new neighbor.
     * 
     * @param peer
     * @return <code>true</code> iff something actually changed.
     */
    private boolean selectAsNeighbour(final IbisIdentifier peer) {
        final boolean needsCatalogMessage;
        if (neighbors.contains(peer)) {
            needsCatalogMessage = !neighbors.haveSentPiecesMessage(peer);
        } else {
            needsCatalogMessage = true;
            if (Settings.TraceScheduler) {
                Globals.log.reportProgress("Scheduler: selected peer " + peer
                        + " as neighbor");
            }
            nonNeighbors.remove(peer);
            neighbors.addPeer(peer, seeding, transmitter);
        }

        if (needsCatalogMessage) {
            sendCatalogMessage(peer);
        }
        return needsCatalogMessage;
    }

    /**
     * Tries to select a new neighbor to get data from.
     * 
     * @return <code>true</code> iff we managed to get a new data source.
     */
    private boolean selectANewNeighbor() {

        if (!nonNeighbors.isEmpty()) {
            // If there is any, give new talent a chance.
            final IbisIdentifier peer = nonNeighbors.extractRandomElement();
            return selectAsNeighbour(peer);
        }
        if (!unrequestedMissingPieces.isEmpty()) {
            return neighbors.addAPeer(transmitter, unrequestedMissingPieces);
        }
        final PieceSet wanted = outstandingPieces
                .getOutstandingPieces(unrequestedMissingPieces.size());
        return neighbors.addAPeer(transmitter, wanted);
    }

    private boolean contains(final PeerInfo l[], final PeerInfo p) {
        for (final PeerInfo e : l) {
            if (e == p) {
                return true;
            }
        }
        return false;
    }

    private boolean fillSlots() {
        final List<PeerInfo> rewardRankedPeers = neighbors
                .getRewardRankedPeers();
        final PeerInfo interestedRewardRankedPeers[] = selectInterestedRankedPeers(rewardRankedPeers);
        final int performanceSlots = Settings.MAXIMAL_UNCHOKED_PEERS - 1;
        /**
         * Try to fill the first n-1 slots with the best peers we have.
         */
        final PeerInfo oldSlots[] = Arrays.copyOf(slots, slots.length);
        Arrays.fill(slots, null);
        for (int i = 0; i < performanceSlots; i++) {
            if (i >= interestedRewardRankedPeers.length) {
                break;
            }
            slots[i] = interestedRewardRankedPeers[i];
        }
        if (interestedRewardRankedPeers.length >= Settings.MAXIMAL_UNCHOKED_PEERS) {
            // Fill the last slot optimistically.
            final int ix = Globals.rng
                    .nextInt(interestedRewardRankedPeers.length
                            - performanceSlots);
            slots[performanceSlots] = interestedRewardRankedPeers[ix
                    + performanceSlots];
        }
        boolean needMorePeers = false;
        for (final PeerInfo peer : slots) {
            if (peer == null) {
                needMorePeers = true;
            } else {
                peer.setWeUnchokedPeer(transmitter, "peer has a slot");
                needMorePeers |= peer.peerHasChokedUs();
            }
        }

        for (final PeerInfo peer : oldSlots) {
            if (peer != null && !contains(slots, peer)) {
                peer.setWeChokedPeer(transmitter, "peer lost its slot");
            }
        }
        return needMorePeers;
    }

    private PeerInfo[] selectInterestedRankedPeers(final List<PeerInfo> l) {
        int n = 0;

        for (final PeerInfo i : l) {
            if (i.peerIsInterested()) {
                n++;
            }
        }
        final PeerInfo res[] = new PeerInfo[n];
        int ix = 0;
        for (final PeerInfo i : l) {
            if (i.peerIsInterested()) {
                res[ix++] = i;
            }
        }
        return res;
    }

    private boolean haveUnusedSlots() {
        for (final PeerInfo p : slots) {
            if (p == null || p.peerHasChokedUs() || p.isDeleted()) {
                return true;
            }
        }
        return false;
    }

    private void maintainNeighbors() {
        final boolean weNeedMoreHelp = haveUnusedSlots();
        boolean newNeighbors = false;
        // Try to maintain a minimum number of neighbors. We may
        // have more active neighbors, but that is based on
        // the idleness of the transmitter.
        if (weNeedMoreHelp
                || neighbors.peerSetIsTooSmall(Settings.NEIGHBOR_SET_SIZE)) {
            while (neighbors
                    .peerSetIsTooSmall(Settings.MAXIMAL_TRACKER_SET_SIZE)) {
                final boolean progress = selectANewNeighbor();
                if (!progress) {
                    // We couldn't make any progress, give up.
                    break;
                }
                newNeighbors = true;
            }
        }
        final long now = System.currentTimeMillis();
        if (newNeighbors
                || lastSlotUpdate + Settings.SLOT_UPDATE_INTERVAL < now) {
            lastSlotUpdate = now;
            fillSlots(); // Ignore 'we need more neighbors flag'.
            maintainOutstandingPieces();
        }
    }

    private void maintainOutstandingPiecesInEndgame() {
        final List<PeerInfo> peersRankedForLeecher = neighbors
                .getRewardRankedPeers();
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("Peer performance for leecher: ");
            for (final PeerInfo peer : peersRankedForLeecher) {
                Globals.log.reportProgress(" "
                        + peer.peer
                        + ": "
                        + Utils.formatByteCount((long) peer
                                .getReceiveTransferRate()));
            }
        }

        for (final PeerInfo p : peersRankedForLeecher) {
            if (p.weAreInterested() && p.hasRoomForRequest()) {
                final int piece = p.getBestPieceToDownload(pieceRanker,
                        outstandingPieces);
                if (piece >= 0) {
                    createPieceRequest(p, piece);
                }
            }
        }
    }

    private void maintainOutstandingPiecesBeforeEndgame() {
        final List<PeerInfo> peersRankedForLeecher = neighbors
                .getRewardRankedPeers();
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("Peer performance for leecher: ");
            for (final PeerInfo peer : peersRankedForLeecher) {
                Globals.log.reportProgress(" "
                        + peer.peer
                        + ": "
                        + Utils.formatByteCount((long) peer
                                .getReceiveTransferRate()));
            }
        }

        for (final PeerInfo p : peersRankedForLeecher) {
            if (p.weAreInterested() && p.hasRoomForRequest()) {
                final int piece = p.getBestPieceToDownload(pieceRanker);
                if (piece >= 0) {
                    createPieceRequest(p, piece);
                } else if (!outstandingPieces.containsPiecesFromPeer(p.peer)) {
                    p.setWeAreUninterested(transmitter, "No pieces we need");
                }
            }
        }
    }

    private void maintainOutstandingPieces() {
        if (seeding) {
            // We're seeding, so we have no outstanding pieces.
            return;
        }
        if (unrequestedMissingPieces.isEmpty()) {
            maintainOutstandingPiecesInEndgame();
        } else {
            maintainOutstandingPiecesBeforeEndgame();
        }
        final long now = System.currentTimeMillis();
        if (lastSlotUpdate + Settings.SLOT_UPDATE_INTERVAL < now) {
            lastSlotUpdate = now;
            final boolean needMorePeers = fillSlots();
            if (needMorePeers) {
                maintainNeighbors();
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
                    .reportProgress("BitTorrentScheduler: instructed engine to download piece "
                            + piece + " from " + peer.peer);
        }
    }

    @Override
    public void addPeer(final IbisIdentifier peer,
            final boolean startCommunicating,
            final PersonalityInterface personality) {
        if (Settings.TraceScheduler) {
            Globals.log.reportProgress("BitTorrentScheduler: new peer " + peer);
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
            Globals.log.reportProgress("BitTorrentScheduler: new active peer "
                    + peer);
        }
        neighbors.addPeer(peer, seeding, transmitter);
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
            Globals.log.reportProgress("BitTorrentScheduler: remove peer "
                    + peer);
        }
        maintainNeighbors(); // Make sure we still have enough neighbors.
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
        maintainNeighbors(); // Make sure we still have enough neighbors.
        return pi;
    }

    @Override
    public void registerPeerHasPiece(final IbisIdentifier source,
            final int piece, final PersonalityInterface personality) {
        final boolean weMayHavePiece = !unrequestedMissingPieces.get(piece);
        final boolean isSeeder = neighbors.peerHasPiece(source, piece,
                weMayHavePiece, seeding, personality, transmitter);
        pieceRanker.addOccurrenceCount(piece);
        if (isSeeder && seeding) {
            disconnectPeer(source);
        }
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
        final boolean isSeeder = neighbors.setBitSet(source, bits, seeding,
                personality, transmitter);
        pieceRanker.addOccurrenceCount(bits);
        if (isSeeder && seeding) {
            disconnectPeer(source);
        }
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
        final OutstandingDownloadingPiece p = outstandingPieces.extractPiece(
                peer, piece);
        pieceRanker.removePendingDownload(piece);
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
        final OutstandingDownloadingPiece p = outstandingPieces.extractPiece(
                peer, piece);
        pieceRanker.removePendingDownload(piece);
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

    private void disconnectPeer(final IbisIdentifier p) {
        nonNeighbors.remove(p);
        neighbors.extractPeer(p);
        bannedPeers.add(p);
        if (Settings.TracePeers) {
            Globals.log
                    .reportProgress("BitTorrentScheduler: disconnected from "
                            + p);
        }
    }

    private void removeSeeders() {
        final List<PeerInfo> l = neighbors.listSeeders();
        for (final PeerInfo p : l) {
            disconnectPeer(p.peer);
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
            Globals.log.reportProgress("BitTorrentScheduler: canceled "
                    + canceled + " extra downloads for piece " + piece);
        }
        if (p != null) {
            if (unrequestedMissingPieces.get(piece)) {
                Globals.log.reportInternalError("Completed piece " + piece
                        + " was listed as unrequested??");
            }
        }
        unrequestedMissingPieces.clear(piece);
        knownPieces.set(piece);
        cancelPieceDownloads(piece);
        if (Settings.TracePieceCount) {
            Globals.log.reportProgress("I now have "
                    + knownPieces.cardinality() + " pieces");
        }
        seeding = knownPieces.isComplete();
        if (seeding) {
            neighbors.stopOurDownloading(transmitter);
            neighbors.setRewardRanker(peerPerformanceRankerForSeeder);
            removeSeeders();
        }
        maintainNeighbors(); // Make sure we still have enough neighbours.
        maintainOutstandingPieces();
    }

    @Override
    public void addChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        neighbors.addChunkRequest(peer, chunk, seeding, transmitter);
    }

    @Override
    public void removeChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        neighbors.removeChunkRequest(peer, chunk);
    }

    @Override
    public ChunkRequest getNextChunkRequest() {
        for (final PeerInfo p : slots) {
            if (p == null) {
                continue;
            }
            final ChunkRequest r = p.getChunkRequest();
            if (r == null) {
                if (Settings.TraceChunkRequests) {
                    Globals.log.reportProgress("Peer " + p
                            + " has no chunk requests");
                }
            } else {
                if (Settings.TraceChunkRequests) {
                    Globals.log.reportProgress("Next granted chunk request is "
                            + r);
                }
                neighbors.waitingRequests--;
                rotateSlots(); // Next time another slot gets priority.
                return r;
            }

        }
        rotateSlots(); // Next time another slot gets priority.
        return null;
    }

    private void rotateSlots() {
        final PeerInfo tmp = slots[0];
        for (int i = 1; i < slots.length; i++) {
            slots[i - 1] = slots[i];
        }
        slots[slots.length - 1] = tmp;
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
        Globals.log.reportProgress("BitTorrentScheduler: seeding=" + seeding);
        Globals.log.reportProgress(knownPieces.cardinality() + " known, "
                + outstandingPieces.size() + " outstanding, "
                + unrequestedMissingPieces.cardinality()
                + " unrequested missing pieces");
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
            if (!bannedPeers.contains(peer)) {
                Globals.log.reportInternalError("Non-neighbor " + peer
                        + " has choked us");
            }
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
        return false;
    }

    @Override
    public void peerHasJoinedAsProxyHelper(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        Globals.log
                .reportInternalError("BitTorrentScheduler: peer sent us a join message: "
                        + peer);
    }

    @Override
    public boolean peerHasResignedAsProxyHelper(final IbisIdentifier peer) {
        Globals.log
                .reportInternalError("BitTorrentScheduler: peer sent us a resignation message: "
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
        disconnectPeer(peer);
    }

    @Override
    public boolean contains(final IbisIdentifier peer) {
        return neighbors.contains(peer) || nonNeighbors.contains(peer)
                || bannedPeers.contains(peer);
    }

    @Override
    public String getName() {
        return "BitTorrent";
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
