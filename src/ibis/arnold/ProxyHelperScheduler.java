package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;

class ProxyHelperScheduler implements SchedulerInterface {
    private final Transmitter transmitter;

    /**
     * The scheduler we use to talk to the outside world.
     */
    private final SchedulerInterface outwardsScheduler;

    /**
     * The scheduler we use to talk to the coordinators.
     */
    private final SchedulerInterface coordinatorsScheduler;

    ProxyHelperScheduler(final SchedulerInterface outwardScheduler,
            final SchedulerInterface coordinatorsScheduler,
            final Transmitter transmitter) {
        this.transmitter = transmitter;
        this.outwardsScheduler = outwardScheduler;
        this.coordinatorsScheduler = coordinatorsScheduler;
    }

    @Override
    public void addChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.addChunkRequest(peer, chunk);
        } else {
            outwardsScheduler.addChunkRequest(peer, chunk);
        }
    }

    @Override
    public void addPeer(final IbisIdentifier peer,
            final boolean startCommunicating,
            final PersonalityInterface personality) {
        // Let it start as an outside peer.
        outwardsScheduler.addPeer(peer, false, personality);
    }

    @Override
    public void addPeer(final PeerInfo peer) {
        outwardsScheduler.addPeer(peer);
    }

    @Override
    public void removeChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.removeChunkRequest(peer, chunk);
        } else {
            outwardsScheduler.removeChunkRequest(peer, chunk);
        }
    }

    @Override
    public void dumpState() {
        final PrintStream s = Globals.log.getPrintStream();
        s.println("------ Proxy Helper: Coordinator statistics ------");
        coordinatorsScheduler.dumpState();
        s.println("------ Proxy Helper: Outwards statistics ------");
        outwardsScheduler.dumpState();
    }

    @Override
    public ChunkRequest getNextChunkRequest() {
        ChunkRequest r = coordinatorsScheduler.getNextChunkRequest();
        if (r == null) {
            r = outwardsScheduler.getNextChunkRequest();
        }
        return r;
    }

    @Override
    public boolean haveIncomingChunkRequests() {
        return coordinatorsScheduler.haveIncomingChunkRequests()
                || outwardsScheduler.haveIncomingChunkRequests();
    }

    @Override
    public void registerCompletedPiece(final IbisIdentifier peer,
            final int piece) {
        coordinatorsScheduler.registerCompletedPiece(peer, piece);
        outwardsScheduler.registerCompletedPiece(peer, piece);
    }

    /**
     * We failed to download the given piece from the given peer, typically
     * because the peer vanished before we were able to get the entire piece.
     * 
     * @param peer
     *            The peer we were downloading from.
     * @param piece
     *            The incorrect piece.
     */
    @Override
    public boolean registerFailedPieceDownload(final IbisIdentifier peer,
            final int piece) {
        final boolean banned;
        if (coordinatorsScheduler.contains(peer)) {
            banned = coordinatorsScheduler.registerFailedPieceDownload(peer,
                    piece);
        } else {
            banned = outwardsScheduler.registerFailedPieceDownload(peer, piece);
        }
        return banned;
    }

    @Override
    public boolean registerIncorrectPiece(final IbisIdentifier peer,
            final int piece) {
        final boolean banned;
        if (coordinatorsScheduler.contains(peer)) {
            banned = coordinatorsScheduler.registerIncorrectPiece(peer, piece);
            // We resign as helper from this coordinator.
            final ResignAsHelperMessage msg = new ResignAsHelperMessage();
            transmitter.addToBookkeepingQueue(peer, msg);
        } else {
            banned = outwardsScheduler.registerIncorrectPiece(peer, piece);
        }
        return banned;
    }

    @Override
    public void registerPeerHasPiece(final IbisIdentifier peer,
            final int piece, final PersonalityInterface personality) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler
                    .registerPeerHasPiece(peer, piece, personality);
        } else {
            outwardsScheduler.registerPeerHasPiece(peer, piece, personality);

        }
    }

    @Override
    public void updateCredit(final IbisIdentifier peer, final CreditValue credit) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.updateCredit(peer, credit);
        } else {
            outwardsScheduler.updateCredit(peer, credit);

        }
    }

    @Override
    public void registerReceivedChunk(final IbisIdentifier peer,
            final int length) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.registerReceivedChunk(peer, length);
        } else {
            outwardsScheduler.registerReceivedChunk(peer, length);
        }
    }

    @Override
    public boolean removePeer(final IbisIdentifier peer) {
        boolean res;
        if (coordinatorsScheduler.contains(peer)) {
            res = coordinatorsScheduler.removePeer(peer);
        } else {
            res = outwardsScheduler.removePeer(peer);
        }
        return res;
    }

    @Override
    public PeerInfo extractPeer(final IbisIdentifier peer) {
        final PeerInfo pi = coordinatorsScheduler.extractPeer(peer);
        if (pi != null) {
            return pi;
        }
        return outwardsScheduler.extractPeer(peer);
    }

    @Override
    public void setPeerHasChokedUs(final IbisIdentifier peer, final boolean flag) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.setPeerHasChokedUs(peer, flag);
        } else {
            outwardsScheduler.setPeerHasChokedUs(peer, flag);
        }
    }

    @Override
    public void setPeerHasPieces(final IbisIdentifier peer,
            final PieceSet bits, final PersonalityInterface personality) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.setPeerHasPieces(peer, bits, personality);
        } else {
            outwardsScheduler.setPeerHasPieces(peer, bits, personality);
        }
    }

    @Override
    public void setPeerIsInterested(final IbisIdentifier peer,
            final boolean flag) {
        if (coordinatorsScheduler.contains(peer)) {
            coordinatorsScheduler.setPeerIsInterested(peer, flag);
        } else {
            outwardsScheduler.setPeerIsInterested(peer, flag);
        }
    }

    @Override
    public boolean generateMoreTransmission() {
        boolean progress = coordinatorsScheduler.generateMoreTransmission();
        if (!progress) {
            progress = outwardsScheduler.generateMoreTransmission();
        }
        return progress;
    }

    @Override
    public void peerHasJoinedAsProxyHelper(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        Globals.log
                .reportInternalError("GreedyScheduler: peer sent us a join message: "
                        + peer);
    }

    @Override
    public boolean peerHasResignedAsProxyHelper(final IbisIdentifier peer) {
        Globals.log
                .reportInternalError("ProxyHelperScheduler: peer sent us a resignation message: "
                        + peer);
        return false;
    }

    @Override
    public int getSourcePeersCount() {
        return coordinatorsScheduler.getSourcePeersCount()
                + outwardsScheduler.getSourcePeersCount();
    }

    @Override
    public boolean askForHelp(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        final PeerInfo pi = outwardsScheduler.extractPeer(peer);
        if (pi != null) {
            coordinatorsScheduler.addPeer(pi);
            final Message msg = new JoinHelpersMessage();
            transmitter.addToBookkeepingQueue(peer, msg);
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Active peer " + peer
                        + " asked for help. Sent accept");
            }
            return true;
        }
        // TODO: don't help peers that we banned.
        outwardsScheduler.removePeer(peer); // Remove any entry.
        coordinatorsScheduler.addPeer(peer, true, personality);
        final Message msg = new JoinHelpersMessage();
        transmitter.addToBookkeepingQueue(peer, msg);
        if (Settings.TraceProxyMode) {
            Globals.log.reportProgress("Inactive peer " + peer
                    + " asked for help. Sent accept");
        }
        return true;
    }

    @Override
    public void handleStoppedHelping(final IbisIdentifier peer) {
        final PeerInfo pi = coordinatorsScheduler.extractPeer(peer);
        if (pi != null) {
            outwardsScheduler.addPeer(pi);
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Peer " + peer
                        + " asked us to stop helping");
            }
        } else {
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Unknown peer " + peer
                        + " asked us to stop helping");
            }
        }
    }

    @Override
    public void handleClosedConnection(final IbisIdentifier peer) {
        coordinatorsScheduler.handleClosedConnection(peer);
        outwardsScheduler.handleClosedConnection(peer);
    }

    @Override
    public void printStatistics(final PrintStream s) {
        s.println("------ Proxy Helper: Coordinator statistics ------");
        coordinatorsScheduler.printStatistics(s);
        s.println("------ Proxy Helper: Outwards statistics ------");
        outwardsScheduler.printStatistics(s);
    }

    @Override
    public boolean contains(final IbisIdentifier peer) {
        return outwardsScheduler.contains(peer)
                || coordinatorsScheduler.contains(peer);
    }

    @Override
    public String getName() {
        return "ProxyHelper";
    }

    @Override
    public void requestPieces(final PieceSet bits) {
        // FIXME: this fails if there is more than
        // one client. Keep track of who requested
        // how much.
        outwardsScheduler.requestPieces(bits);
    }

    @Override
    public void shutdown() {
        coordinatorsScheduler.shutdown();
        outwardsScheduler.shutdown();
    }
}
