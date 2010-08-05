package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.ArrayList;

@SuppressWarnings("synthetic-access")
final class ProxyCoordinatorScheduler implements SchedulerInterface {
    private final PeerSet nonHelpers = new PeerSet();
    private final PeerSet helpers = new PeerSet();
    private final PeerSet candidateHelpers = new PeerSet();
    private final SchedulerInterface helpersScheduler;
    private final Transmitter transmitter;
    private final RecordedPeerInfoList recordedPeerInfo;
    private final ProxyPiecesSelector piecesSelector;

    private static class RecordedPeerInfo {
        private final IbisIdentifier peer;
        private PieceSet pieces;
        private boolean peerHasChokedUs;
        private boolean peerIsInterested;
        private CreditValue credit = new CreditValue(0, -1);

        private RecordedPeerInfo(final IbisIdentifier peer,
                final int numberOfPieces) {
            this.peer = peer;
            pieces = new PieceSet(numberOfPieces);
        }

        private RecordedPeerInfo(final IbisIdentifier peer,
                final PieceSet pieces) {
            this.peer = peer;
            this.pieces = pieces;
        }

        private void updateScheduler(final SchedulerInterface s,
                final PersonalityInterface personality) {
            s.setPeerHasPieces(peer, pieces, personality);
            s.setPeerHasChokedUs(peer, peerHasChokedUs);
            s.setPeerIsInterested(peer, peerIsInterested);
            s.updateCredit(peer, credit);
        }

        private void updateCredit(final CreditValue cv) {
            this.credit = this.credit.update(cv);
        }
    }

    private static class RecordedPeerInfoList {
        private final int numberOfPieces;
        private final ArrayList<RecordedPeerInfo> l = new ArrayList<RecordedPeerInfo>();

        private RecordedPeerInfoList(final int numberOfPieces) {
            this.numberOfPieces = numberOfPieces;
        }

        private int searchPeer(final IbisIdentifier peer) {
            int ix = l.size();
            while (ix > 0) {
                ix--;
                final RecordedPeerInfo info = l.get(ix);
                if (info.peer.equals(peer)) {
                    return ix;
                }
            }
            return -1;
        }

        private void removePeer(final IbisIdentifier peer) {
            final int ix = searchPeer(peer);
            if (ix >= 0) {
                l.remove(ix);
            }
        }

        private RecordedPeerInfo extractPeerInfo(final IbisIdentifier peer) {
            final int ix = searchPeer(peer);
            if (ix >= 0) {
                return l.remove(ix);
            }
            return null;
        }

        private void setPeerIsInterested(final IbisIdentifier peer,
                final boolean flag) {
            int ix = searchPeer(peer);
            if (ix < 0) {
                ix = l.size();
                l.add(new RecordedPeerInfo(peer, numberOfPieces));
            }
            final RecordedPeerInfo info = l.get(ix);
            info.peerIsInterested = flag;
        }

        private void setPeerHasChokedUs(final IbisIdentifier peer,
                final boolean flag) {
            int ix = searchPeer(peer);
            if (ix < 0) {
                ix = l.size();
                l.add(new RecordedPeerInfo(peer, numberOfPieces));
            }
            final RecordedPeerInfo info = l.get(ix);
            info.peerHasChokedUs = flag;
        }

        private void registerPeerHasPiece(final IbisIdentifier peer,
                final int piece) {
            int ix = searchPeer(peer);
            if (ix < 0) {
                ix = l.size();
                l.add(new RecordedPeerInfo(peer, numberOfPieces));
            }
            final RecordedPeerInfo info = l.get(ix);
            info.pieces.set(piece);
        }

        private void setPeerHasPieces(final IbisIdentifier peer,
                final PieceSet pieces) {
            final int ix = searchPeer(peer);
            if (ix < 0) {
                l.add(new RecordedPeerInfo(peer, pieces));
            } else {
                final RecordedPeerInfo info = l.get(ix);
                info.pieces = pieces;
            }
        }

        private void updateCredit(final IbisIdentifier peer,
                final CreditValue credit) {
            final int ix = searchPeer(peer);
            if (ix >= 0) {
                final RecordedPeerInfo info = l.get(ix);
                info.updateCredit(credit);
            }
        }
    }

    /**
     * Constructs a new proxy coordinator scheduler, using the given schedulers
     * to talk to the helpers and outside peers respectively.
     * 
     * @param helpersScheduler
     *            The scheduler for helpers.
     * @param transmitter
     *            The transmitter.
     */
    ProxyCoordinatorScheduler(final SchedulerInterface helpersScheduler,
            final Transmitter transmitter, final int numberOfPieces,
            final PieceSet knownPieces) {
        this.helpersScheduler = helpersScheduler;
        this.transmitter = transmitter;
        this.piecesSelector = new RoundRobinProxyPiecesSelector(transmitter,
                numberOfPieces, knownPieces);
        recordedPeerInfo = new RecordedPeerInfoList(numberOfPieces);
    }

    private void maintainHelperPool() {
        if (candidateHelpers.isEmpty() && !nonHelpers.isEmpty()
                && piecesSelector.needMoreHelpers()) {
            final IbisIdentifier newCandidate = nonHelpers
                    .extractRandomElement();
            candidateHelpers.add(newCandidate);
            // FIXME: implement a timeout.
            final Message msg = new AskForHelpMessage();
            transmitter.addToBookkeepingQueue(newCandidate, msg);
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Asked " + newCandidate
                        + " for help nonHelpers=" + nonHelpers
                        + " candidateHelpers=" + candidateHelpers);
            }
        }
    }

    @Override
    public void addChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        if (nonHelpers.contains(peer)) {
            Globals.log
                    .reportInternalError("Ignoring unprovoked chunk request from "
                            + peer);
            final Message msg = new CloseConnectionMessage();
            transmitter.addToBookkeepingQueue(peer, msg);
        } else {
            helpersScheduler.addChunkRequest(peer, chunk);
        }
    }

    @Override
    public void addPeer(final IbisIdentifier peer,
            final boolean startCommunicating,
            final PersonalityInterface personality) {
        nonHelpers.add(peer);
        if (Settings.TraceProxyMode) {
            Globals.log.reportProgress("Added " + peer
                    + " to non-helpers: nonHelpers=" + nonHelpers);
        }
        maintainHelperPool();
    }

    @Override
    public void removeChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        helpersScheduler.removeChunkRequest(peer, chunk);
    }

    @Override
    public void dumpState() {
        Globals.log.reportProgress("nonHelpers=" + nonHelpers);
        Globals.log.reportProgress("helpers=" + helpers);
        helpersScheduler.dumpState();
    }

    @Override
    public ChunkRequest getNextChunkRequest() {
        return helpersScheduler.getNextChunkRequest();
    }

    @Override
    public boolean haveIncomingChunkRequests() {
        return helpersScheduler.haveIncomingChunkRequests();
    }

    @Override
    public void printStatistics(final PrintStream s) {
        helpersScheduler.printStatistics(s);
    }

    @Override
    public void registerCompletedPiece(final IbisIdentifier peer,
            final int piece) {
        helpersScheduler.registerCompletedPiece(peer, piece);
        piecesSelector.havePiece(piece);
    }

    @Override
    public boolean registerFailedPieceDownload(final IbisIdentifier peer,
            final int piece) {
        final boolean removed = helpersScheduler.registerFailedPieceDownload(
                peer, piece);
        maintainHelperPool();
        return removed;
    }

    @Override
    public boolean registerIncorrectPiece(final IbisIdentifier peer,
            final int piece) {
        final boolean banned = helpersScheduler.registerIncorrectPiece(peer,
                piece);
        maintainHelperPool();
        return banned;
    }

    @Override
    public void registerPeerHasPiece(final IbisIdentifier peer,
            final int piece, final PersonalityInterface personality) {
        if (helpers.contains(peer)) {
            helpersScheduler.registerPeerHasPiece(peer, piece, personality);
        } else {
            recordedPeerInfo.registerPeerHasPiece(peer, piece);
        }
    }

    @Override
    public void registerReceivedChunk(final IbisIdentifier peer,
            final int length) {
        helpersScheduler.registerReceivedChunk(peer, length);
    }

    @Override
    public boolean removePeer(final IbisIdentifier peer) {
        nonHelpers.remove(peer);
        candidateHelpers.remove(peer);
        if (helpers.remove(peer)) {
            piecesSelector.removeHelper(peer);
        }
        recordedPeerInfo.removePeer(peer);
        final boolean changed = helpersScheduler.removePeer(peer);
        maintainHelperPool();
        return changed;
    }

    @Override
    public void setPeerHasChokedUs(final IbisIdentifier peer, final boolean flag) {
        if (helpers.contains(peer)) {
            helpersScheduler.setPeerHasChokedUs(peer, flag);
        } else {
            recordedPeerInfo.setPeerHasChokedUs(peer, flag);
        }
    }

    @Override
    public void setPeerHasPieces(final IbisIdentifier peer,
            final PieceSet bits, final PersonalityInterface personality) {
        if (helpers.contains(peer)) {
            helpersScheduler.setPeerHasPieces(peer, bits, personality);
        } else {
            recordedPeerInfo.setPeerHasPieces(peer, bits);
        }
    }

    @Override
    public void updateCredit(final IbisIdentifier peer, final CreditValue credit) {
        if (helpers.contains(peer)) {
            helpersScheduler.updateCredit(peer, credit);
        } else {
            recordedPeerInfo.updateCredit(peer, credit);
        }
    }

    @Override
    public void setPeerIsInterested(final IbisIdentifier peer,
            final boolean flag) {
        if (helpers.contains(peer)) {
            helpersScheduler.setPeerIsInterested(peer, flag);
        } else {
            recordedPeerInfo.setPeerIsInterested(peer, flag);
        }
    }

    @Override
    public boolean generateMoreTransmission() {
        maintainHelperPool();
        return helpersScheduler.generateMoreTransmission();
    }

    @Override
    public void peerHasJoinedAsProxyHelper(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        final boolean changed = candidateHelpers.remove(peer);
        if (Settings.TraceProxyMode) {
            Globals.log
                    .reportProgress("peerHasJoinedAsProxyHelper: candidateHelpers="
                            + candidateHelpers);
        }
        if (changed) {
            final RecordedPeerInfo info = recordedPeerInfo
                    .extractPeerInfo(peer);
            helpersScheduler.addPeer(peer, true, personality);
            personality.dumpState();
            helpers.add(peer);
            piecesSelector.addHelper(peer);
            info.updateScheduler(helpersScheduler, personality);
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Peer " + peer + " is now a helper");
                personality.dumpState();
            }
        } else {
            Globals.log
                    .reportInternalError("Non-candidate helper tried to join as helper: "
                            + peer + ", candidateHelpers=" + candidateHelpers);
        }
        maintainHelperPool();
    }

    @Override
    public boolean peerHasResignedAsProxyHelper(final IbisIdentifier peer) {
        recordedPeerInfo.removePeer(peer);
        boolean changed = candidateHelpers.remove(peer);
        if (changed) {
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Candidate helper " + peer
                        + " rejected help request: candidateHelpers="
                        + candidateHelpers);
            }
        } else {
            changed = helpersScheduler.removePeer(peer);
            if (changed) {
                piecesSelector.removeHelper(peer);
            }
            if (Settings.TraceProxyMode) {
                Globals.log.reportProgress("Helper " + peer + " resigned");
            }
        }
        maintainHelperPool();
        return changed;
    }

    @Override
    public int getSourcePeersCount() {
        return helpersScheduler.getSourcePeersCount();
    }

    @Override
    public boolean askForHelp(final IbisIdentifier peer,
            final PersonalityInterface personality) {
        final Message msg = new ResignAsHelperMessage();
        transmitter.addToBookkeepingQueue(peer, msg);
        if (Settings.TraceProxyMode) {
            Globals.log
                    .reportProgress("Peer "
                            + peer
                            + " asked for help, but I'm a coordinator myself. Sent reject");
        }
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
        nonHelpers.remove(peer);
        candidateHelpers.remove(peer);
        recordedPeerInfo.removePeer(peer);
        helpersScheduler.handleClosedConnection(peer);
        Globals.log.reportProgress("Peer " + peer + " closed connection");
        maintainHelperPool();
    }

    @Override
    public void addPeer(final PeerInfo peer) {
        helpersScheduler.addPeer(peer);
    }

    @Override
    public PeerInfo extractPeer(final IbisIdentifier peer) {
        return helpersScheduler.extractPeer(peer);
    }

    @Override
    public boolean contains(final IbisIdentifier peer) {
        return helpersScheduler.contains(peer)
                || candidateHelpers.contains(peer) || nonHelpers.contains(peer);
    }

    @Override
    public String getName() {
        return "ProxyCoordinator";
    }

    @Override
    public void requestPieces(final PieceSet bits) {
        Globals.log.reportInternalError("Requested piece(s) from coordinator");
    }

    @Override
    public void shutdown() {
        helpersScheduler.shutdown();
    }

}
