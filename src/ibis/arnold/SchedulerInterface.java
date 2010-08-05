package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;

/**
 * The interface of the scheduler. That is, the engine that determines which
 * pieces should be downloaded from which peer.
 * 
 * @author Kees van Reeuwijk
 * 
 */
interface SchedulerInterface {
    /**
     * A new peer is known to the system.
     * 
     * @param peer
     *            The peer.
     * @param startAsNeighbor
     *            If <code>true</code>, make this peer immediately a neighbor we
     *            communicate with, instead of just a candidate for this rank.
     * @param personality
     *            The personality that needs to be informed about new peers.
     */
    void addPeer(IbisIdentifier peer, boolean startAsNeighbor,
            PersonalityInterface personality);

    /**
     * Add the given peer as a neighbor (active peer) to the scheduler.
     * 
     * @param peer
     *            The peer.
     */
    void addPeer(PeerInfo peer);

    /**
     * The given peer has gone from the system.
     * 
     * @param peer
     *            The peer that is now gone.
     * @return <code>true</code> iff there really was a peer to remove.
     */
    boolean removePeer(IbisIdentifier peer);

    /**
     * Remove the given peer from the list of active peers, and return its
     * information.
     * 
     * @param peer
     *            The peer to remove.
     * @return The information of the peer, or <code>null</code> if the peer
     *         does not exist or is not active.
     */
    PeerInfo extractPeer(IbisIdentifier peer);

    /**
     * We now know that the given peer has the given piece.
     * 
     * @param peer
     *            The peer that has the piece.
     * @param piece
     *            The piece that the peer owns.
     * @param personality
     *            The personality that should be updated.
     */
    void registerPeerHasPiece(IbisIdentifier peer, int piece,
            PersonalityInterface personality);

    /**
     * We now know that the given peer has (only) the given set of pieces.
     * 
     * @param peer
     *            The peer for which this is the set.
     * @param bits
     *            The set of known pieces of the peer.
     * @param personality
     */
    void setPeerHasPieces(IbisIdentifier peer, PieceSet bits,
            PersonalityInterface personality);

    /**
     * The given peer has send us the given incorrect piece.
     * 
     * @param peer
     *            The peer that sent us the piece.
     * @param piece
     *            The incorrect piece.
     * @return <code>true</code> iff the peer was banned for this incorrect
     *         piece.
     */
    boolean registerIncorrectPiece(IbisIdentifier peer, int piece);

    /**
     * We failed to download the given piece from the given peer, typically
     * because the peer vanished before we were able to get the entire piece.
     * 
     * @param peer
     *            The peer we were downloading from.
     * @param piece
     *            The incorrect piece.
     * @return <code>true</code> iff the peer was banned for this failed
     *         download.
     */
    boolean registerFailedPieceDownload(IbisIdentifier peer, int piece);

    /**
     * The peer <code>peer</code> has sent us the entire piece
     * <code>piece</code>.
     * 
     * @param peer
     *            The peer that sent us the piece.
     * @param piece
     *            The piece.
     */
    void registerCompletedPiece(IbisIdentifier peer, int piece);

    void addChunkRequest(IbisIdentifier peer, Chunk chunk);

    void removeChunkRequest(IbisIdentifier peer, Chunk chunk);

    /**
     * Returns the next chunk request from a remote peer that should be handled.
     * 
     * @return The chunk request.
     */
    ChunkRequest getNextChunkRequest();

    void registerReceivedChunk(IbisIdentifier peer, int length);

    /**
     * Returns true iff the scheduler has new chunk requests to submit to the
     * engine.
     * 
     * @return <code>true</code> iff there are chunk requests to submit.
     */
    boolean haveIncomingChunkRequests();

    public void printStatistics(final PrintStream s);

    public void dumpState();

    /**
     * Registers that the given peer has allowed or not allowed us to send piece
     * requests.
     * 
     * @param peer
     *            The peer that has communicated with us.
     * @param flag
     *            <code>true</code> if we should not send requests,
     *            <code>false</code> if requests are OK.
     */
    void setPeerHasChokedUs(IbisIdentifier peer, boolean flag);

    /**
     * Registers whether the given peer is interested in sending piece requests.
     * 
     * @param peer
     *            The peer that has communicated with us.
     * @param flag
     *            <code>true</code> if the peer would want to send piece
     *            requests, <code>false</code> if the peer is not interested in
     *            sending piece requests.
     */
    void setPeerIsInterested(IbisIdentifier peer, boolean flag);

    /**
     * Registers that the transmitter has not enough requests to fulfill, so
     * more transmissions should be generated.
     * 
     * @return <code>true</code> if some adjustments were made.
     */
    boolean generateMoreTransmission();

    /**
     * Registers that the given peer joins us as a proxy helper.
     * 
     * @param peer
     *            The peer that sent us the join message.
     * @param personality
     *            The personality that wants to be informed of changes.
     */
    void peerHasJoinedAsProxyHelper(IbisIdentifier peer,
            PersonalityInterface personality);

    /**
     * Registers that the given peer has sent us a helper resignation message.
     * 
     * @param peer
     *            The peer that sent us the resignation message.
     * @return <code>true</code> iff the peer was known in the first place.
     */
    boolean peerHasResignedAsProxyHelper(IbisIdentifier peer);

    /**
     * Returns the number of peers that we're actively downloading from.
     * 
     * @return The number of downloading peers.
     */
    int getSourcePeersCount();

    /**
     * The given peer wants help for downloading.
     * 
     * @param peer
     *            The peer that wants help.
     * @param personality
     *            The personality of the node.
     * @return <code>true</code> iff we are going to help.
     */
    boolean askForHelp(IbisIdentifier peer, PersonalityInterface personality);

    /**
     * The given peer tells us it no longer needs help.
     * 
     * @param peer
     *            The peer that doesn't want help any more.
     */
    void handleStoppedHelping(IbisIdentifier peer);

    /**
     * The given peer tells us that it no longer wants to talk to us.
     * 
     * @param peer
     *            The peer that closed the connection.
     */
    void handleClosedConnection(IbisIdentifier peer);

    /**
     * Returns <code>true</code> iff this scheduler contains (controls) the
     * given peer.
     * 
     * @param peer
     *            The peer we're searching for.
     * @return <code>true</code> iff the scheduler contains the peer.
     */
    boolean contains(IbisIdentifier peer);

    /**
     * Returns the name of this scheduler.
     * 
     * @return The name.
     */
    String getName();

    /**
     * Requests the scheduler to download only the given set of pieces. This
     * call supersedes all previous specifications of the set.
     * 
     * @param set
     *            The set of pieces to download.
     */
    void requestPieces(PieceSet set);

    /**
     * Registers that the given per has the given credit.
     * 
     * @param peer
     *            The peer
     * @param credit
     *            The new credit value.
     */
    void updateCredit(IbisIdentifier peer, CreditValue credit);

    /**
     * Do all that is needed for a clean shutdown.
     */
    void shutdown();
}
