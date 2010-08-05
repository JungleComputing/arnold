package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The list of known peers.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class PeerInfoList {
    private final RankedPeerList rewardRankedPeers;
    private final RankedPeerList downloadSpeedRankedPeers;
    int waitingRequests = 0;
    private int maximalWaitingRequests = 0;
    private final int numberOfPieces;

    PeerInfoList(final int numberOfPieces) {
        this.numberOfPieces = numberOfPieces;
        final PeerRanker downloadSpeedRanker = new PeerRankerReceivePerformance();
        rewardRankedPeers = new RankedPeerList(downloadSpeedRanker);
        downloadSpeedRankedPeers = new RankedPeerList(downloadSpeedRanker);
    }

    void addPeer(final PeerInfo peerInfo, final boolean weAreSeeding,
            final Transmitter transmitter) {
        if (Settings.TracePeers) {
            Globals.log.reportProgress("Added peer " + peerInfo.peer
                    + " to neighbor list");
        }

        if (weAreSeeding) {
            peerInfo.setWeAreUninterested(transmitter, "we are seeding");
        } else {
            peerInfo.setWeAreInterested(transmitter,
                    "new peer may have interesting pieces");
        }
        downloadSpeedRankedPeers.add(peerInfo);
        rewardRankedPeers.add(peerInfo);
    }

    PeerInfo addPeer(final IbisIdentifier peer, final boolean weAreSeeding,
            final Transmitter transmitter) {
        final PeerInfo p = new PeerInfo(peer, numberOfPieces);
        addPeer(p, weAreSeeding, transmitter);
        return p;
    }

    /**
     * Removes the given peer from our administration, and returns the
     * information we were keeping on it. Presumably this info will be added to
     * another list of peers.
     * 
     * @param peer
     *            The peer to extract.
     * @return The <code>PeerInfo</code> of the removed peer.
     */
    PeerInfo extractPeer(final IbisIdentifier peer) {
        final int ix = rewardRankedPeers.findPeer(peer, true);
        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.remove(ix);
            if (Settings.TracePeers) {
                Globals.log.reportProgress("Extracted peer " + peer);
            }
            downloadSpeedRankedPeers.remove(p);
            return p;
        }
        if (Settings.TracePeers) {
            Globals.log.reportProgress(true, "Cannot extract unknown peer "
                    + peer);
        }
        return null;
    }

    /**
     * Removes the given peer from our administration, since it is gone. Returns
     * the PeerInfo of the removed peer.
     * 
     * @param peer
     *            The peer to remove.
     * @param registerRemovedPeer
     *            If <code>true</code> remember this peer in a special list of
     *            removed peers, so that it statistics can be printed.
     * @return The <code>PeerInfo</code> of the removed peer.
     */
    PeerInfo removePeer(final IbisIdentifier peer) {
        final PeerInfo p = extractPeer(peer);
        if (p != null) {
            p.setDeleted();
            if (Settings.TracePeers) {
                Globals.log.reportProgress("Marked peer " + peer
                        + " as deleted");
            }
        }
        return p;
    }

    void disconnectPeer(final IbisIdentifier p, final Transmitter transmitter) {
        final Message msg = new CloseConnectionMessage();
        transmitter.addToBookkeepingQueue(p, msg);
    }

    /**
     * Set the set of known pieces of the given peer to the given set.
     * 
     * @param peer
     *            The peer to set the bitset for.
     * @param bits
     *            The set of known pieces of the peer.
     * @param weAreSeeder
     *            If set, the local peer is a seeder.
     * @param personality
     *            Our personality.
     * @param transmitter
     *            The transmitter that will handle any messages to the peer.
     */
    boolean setBitSet(final IbisIdentifier peer, final PieceSet bits,
            final boolean weAreSeeder, final PersonalityInterface personality,
            final Transmitter transmitter) {
        final int ix = rewardRankedPeers.findPeer(peer, true);
        boolean isSeeder = false;

        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            p.setKnownPieces(bits);
            isSeeder = p.isSeeder();
            if (isSeeder) {
                // This peer is a seeder.
                personality.peerIsSeeder(p.peer);
                // Choke it; it is not interested in data.
                p.setWeChokedPeer(transmitter, "is seeder");
            }
            if (!weAreSeeder && !bits.isEmpty()) {
                p.setWeAreInterested(transmitter, "learned about new pieces");
            }
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
        } else {
            Globals.log.reportInternalError("Unknown peer " + peer
                    + " sent us it bitset");
        }
        return isSeeder;
    }

    boolean peerHasPiece(final IbisIdentifier peer, final int piece,
            final boolean weMayHavePiece, final boolean weAreSeeding,
            final PersonalityInterface personality,
            final Transmitter transmitter) {
        final int ix = rewardRankedPeers.findPeer(peer, false);

        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            final boolean isNowSeed = p.registerHasPiece(piece);
            if (isNowSeed) {
                personality.peerIsSeeder(peer);
                p.setWeChokedPeer(transmitter, "is seeder");
                if (!weAreSeeding) {
                    p.setWeAreInterested(transmitter, "is seeder");
                }
            } else if (!weMayHavePiece && !p.weChokedPeer()) {
                p.setWeAreInterested(transmitter, "peer has interesting piece "
                        + piece);
            }
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
            return isNowSeed;
        }
        // Now also search for deleted peers.
        final int i = rewardRankedPeers.findPeer(peer, true);
        if (i < 0) {
            // Not even a deleted peer.
            Globals.log.reportInternalError("Unknown peer " + peer
                    + " reported it has piece " + piece);
        }
        return false;
    }

    void cancelPiece(final IbisIdentifier peer, final int piece) {
        final int ix = rewardRankedPeers.findPeer(peer, true);

        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            p.registerCanceledPiece(piece);
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
        }
    }

    void registerCompletedPiece(final IbisIdentifier peer,
            final Transmitter transmitter, final int piece) {
        final int ix = rewardRankedPeers.findPeer(peer, true);

        PeerInfo res = null;
        if (ix >= 0) {
            res = rewardRankedPeers.get(ix);
            res.registerCompletedPiece(piece);
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(res);
        }

        // Now tell all our neighbours we have this piece.
        HaveMessage msg = null;
        for (final PeerInfo p : rewardRankedPeers) {
            if (p.needsHaveMessage()) {
                if (msg == null) {
                    // We really need this message, make it.
                    msg = new HaveMessage(piece);
                }
                transmitter.addToBookkeepingQueue(p.peer, msg);
            }
        }
    }

    void updateCredit(final IbisIdentifier peer, final CreditValue credit) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            if (p.updateCredit(credit)) {
                rewardRankedPeers.updateRanking(ix);
                downloadSpeedRankedPeers.updateRanking(p);
            }
        }
    }

    void registerCanceledPiece(final IbisIdentifier peer, final int piece) {
        final int ix = rewardRankedPeers.findPeer(peer, true);

        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            p.registerCanceledPiece(piece);
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
        } else {
            Globals.log.reportInternalError("Unknown peer " + peer
                    + " canceled piece " + piece);
        }
    }

    /**
     * Returns true iff the given peer is member of this neighbor list.
     * 
     * @param peer
     *            The peer.
     * @return True iff the peer is member of this list.
     */
    boolean contains(final IbisIdentifier peer) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        return ix >= 0;
    }

    void registerReceivedChunk(final IbisIdentifier peer, final long length) {
        final int ix = rewardRankedPeers.findPeer(peer, false);

        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            p.registerReceivedChunk(length);
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
        }
    }

    List<PeerInfo> getRewardRankedPeers() {
        return rewardRankedPeers.getList();
    }

    List<PeerInfo> getDownloadSpeedRankedPeers() {
        return downloadSpeedRankedPeers.getList();
    }

    void addChunkRequest(final IbisIdentifier peer, final Chunk chunk,
            final boolean weAreSeeding, final Transmitter transmitter) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        final PeerInfo p;

        if (ix >= 0) {
            p = rewardRankedPeers.get(ix);
        } else {
            final int i = rewardRankedPeers.findPeer(peer, true);
            if (i < 0) {
                p = addPeer(peer, weAreSeeding, transmitter);
                if (Settings.TracePeers) {
                    Globals.log
                            .reportProgress("Peer added "
                                    + peer
                                    + " to neighbor list because it sent a chunk request");
                }
            } else {
                Globals.log
                        .reportInternalError("Ignored chunk request from deleted peer "
                                + peer + "; it requested " + chunk);
                return;
            }
        }
        p.addChunkRequest(chunk);
        waitingRequests++;
        if (waitingRequests > maximalWaitingRequests) {
            maximalWaitingRequests = waitingRequests;
        }
    }

    void removeChunkRequest(final IbisIdentifier peer, final Chunk chunk) {
        final int ix = rewardRankedPeers.findPeer(peer, true);

        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            final boolean found = p.removeChunkRequest(chunk);
            if (found) {
                waitingRequests--;
            }
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
        }
    }

    /**
     * Returns the next chunk request to fulfill. We give priority to chunks
     * from our best-ranked peers.
     * 
     * @param ranker
     *            The ranker that determines who gets priority.
     * 
     * @return The next chunk request to fulfill, or <code>null</code> if there
     *         are no requests.
     */
    ChunkRequest getNextChunkRequest() {
        final List<PeerInfo> rankedPeers = getRewardRankedPeers();
        for (final PeerInfo p : rankedPeers) {
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
                waitingRequests--;
                return r;
            }
        }
        return null;
    }

    /**
     * Returns <code>true</code> iff there are any chunk requests in our
     * administration. That is return <code>true</code> iff
     * <code>getNextChunkRequest()</code> would return a non-<code>null</code>
     * result.
     * 
     * @return <code>true</code> iff there are any chunks to get.
     */
    boolean haveIncomingChunkRequests() {
        return waitingRequests > 0;
    }

    /**
     * Registers that the given peer has set our choked state to the given
     * value.
     * 
     * @param peer
     *            The peer that has sent us a choke/unchoke message.
     * @param flag
     *            The new choke state.
     * @return <code>true</code> iff this peer is a neighbor.
     */
    boolean setPeerHasChokedUs(final IbisIdentifier peer, final boolean flag) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        if (ix < 0) {
            // Not a neighbor.
            return false;
        }
        final PeerInfo p = rewardRankedPeers.get(ix);
        p.setPeerHasChokedUs(flag);
        rewardRankedPeers.updateRanking(ix);
        downloadSpeedRankedPeers.updateRanking(p);
        return true;
    }

    void setPeerIsInterested(final IbisIdentifier peer, final boolean flag) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        if (ix >= 0) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            p.setPeerIsInterestedInUs(flag);
            rewardRankedPeers.updateRanking(ix);
            downloadSpeedRankedPeers.updateRanking(p);
        }
    }

    void setPeerWantsToTalkToUs(final IbisIdentifier peer, final boolean flag) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        if (ix < 0) {
            // Not a neighbour.
            return;
        }
        final PeerInfo p = rewardRankedPeers.get(ix);
        p.setPeerWantsToTalkToUs(flag);
        rewardRankedPeers.updateRanking(ix);
        downloadSpeedRankedPeers.updateRanking(p);
    }

    int size() {
        return rewardRankedPeers.size();
    }

    boolean peerSetIsTooSmall(final int minimalPeerCount) {
        final int sourcePeers = size();
        return sourcePeers < minimalPeerCount;
    }

    /**
     * Tries to develop a new neighbor to get data from.
     * 
     * @param transmitter
     *            The transmitter.
     * @return <code>true</code> iff we managed to get a new data source.
     */
    boolean addAPeer(final Transmitter transmitter, final PieceSet missingPieces) {
        final int numberOfPeers = rewardRankedPeers.size();
        if (numberOfPeers < 1) {
            return false;
        }
        int ix = Globals.rng.nextInt(numberOfPeers);
        for (int i = 0; i < numberOfPeers; i++) {
            final PeerInfo p = rewardRankedPeers.get(ix);
            if (!p.isDeleted() && !p.weAreInterested()
                    && p.hasSomeOfOurMissingPieces(missingPieces)) {
                // We are now interested in this peer.
                p.setWeAreInterested(transmitter, "we need more data sources");
                if (p.isSeeder()) {
                    p.setWeChokedPeer(transmitter, "seeder doesn't need us");
                } else {
                    p.setWeUnchokedPeer(transmitter, "tit-for-tat");
                }
                rewardRankedPeers.updateRanking(ix);
                downloadSpeedRankedPeers.updateRanking(p);
                return true;
            }
            ix++;
            if (ix >= numberOfPeers) {
                // If necessary wrap around.
                ix = 0;
            }
        }
        return false;
    }

    /**
     * Disable any of our downloading or interests, we are no longer interested.
     * 
     * @param transmitter
     *            The transmitter that will handle any messages that have to be
     *            sent.
     */
    void stopOurDownloading(final Transmitter transmitter) {
        for (final PeerInfo p : rewardRankedPeers) {
            if (!p.isDeleted()) {
                p.setWeAreUninterested(transmitter, "we are seeder");
            }
        }
    }

    /**
     * Given a peer, returns true iff we have sent our pieces message to this
     * peer.
     * 
     * @param peer
     *            The peer we're inquiring about.
     * @return True iff we have sent our pieces message to this peer.
     */
    boolean haveSentPiecesMessage(final IbisIdentifier peer) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        if (ix < 0) {
            Globals.log
                    .reportInternalError("Trying to get needsHaveMessage for unknown peer "
                            + peer);
            return false;
        }
        final PeerInfo p = rewardRankedPeers.get(ix);
        return p.haveSentPiecesMessage();
    }

    void setSentCatalog(final IbisIdentifier peer) {
        final int ix = rewardRankedPeers.findPeer(peer, false);
        if (ix < 0) {
            Globals.log.reportInternalError("Sent catalog to unknown peer "
                    + peer + "??");
            return;
        }
        final PeerInfo p = rewardRankedPeers.get(ix);
        p.setSentPiecesMessage();
    }

    void printStatistics(final PrintStream s) {
        if (rewardRankedPeers.isEmpty()) {
            Globals.log.reportProgress("(no peers)");
            return;
        }
        s.println("maximalWaitingRequests=" + maximalWaitingRequests);
        s.println("rx pieces  cancel pc rx bytes  tx bytes  peer");
        for (final PeerInfo p : rewardRankedPeers) {
            p.printStatistics(s);
        }
        s.println(" Chunk request fulfillment:");
        for (final PeerInfo p : rewardRankedPeers) {
            p.dumpFulfillmentStatistics(s);
        }
    }

    void dumpState() {
        if (rewardRankedPeers.isEmpty()) {
            Globals.log.reportProgress("(no peers)");
            return;
        }

        Globals.log.reportProgress("waitingRequests=" + waitingRequests
                + " maximalWaitingRequests=" + maximalWaitingRequests);
        Globals.log.reportProgress("Peer state:");
        final PrintStream printStream = Globals.log.getPrintStream();
        for (final PeerInfo p : rewardRankedPeers) {
            if (!p.isDeleted()) {
                p.dumpPeerState(printStream);
            }
        }
        printStream
                .println("rx pieces  cl pieces  rx bytes  tx bytes      rate        rate    peer");
        for (final PeerInfo p : rewardRankedPeers) {
            p.dumpState(printStream);
        }
        Globals.log.reportProgress(" Received chunk requests:");
        for (final PeerInfo p : rewardRankedPeers) {
            p.dumpChunkRequests(printStream);
        }
        Globals.log.reportProgress(" Outstanding chunk requests:");
        for (final PeerInfo p : rewardRankedPeers) {
            p.dumpOutstandingRequests(printStream);
        }
        Globals.log.reportProgress(" Chunk request fulfillment:");
        for (final PeerInfo p : rewardRankedPeers) {
            p.dumpFulfillmentStatistics(printStream);
        }
    }

    void shutdown(final Transmitter transmitter) {
        for (final PeerInfo peer : rewardRankedPeers) {
            if (peer.needsShutdownMessage()) {
                final CloseConnectionMessage msg = new CloseConnectionMessage();
                transmitter.addToBookkeepingQueue(peer.peer, msg);
            }
        }
    }

    void setRewardRanker(final PeerRanker r) {
        rewardRankedPeers.setRanker(r);
    }

    List<PeerInfo> listSeeders() {
        final ArrayList<PeerInfo> res = new ArrayList<PeerInfo>();
        for (final PeerInfo p : rewardRankedPeers) {
            if (p.isSeeder()) {
                res.add(p);
            }
        }
        return res;
    }
}
