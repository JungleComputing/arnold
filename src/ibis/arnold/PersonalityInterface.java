package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;

/**
 * The interface to the personality of a peer. Instances implementing this
 * interface cause the peer to behave in a particular manner wrt seeding and in
 * the future perhaps also download and upload speed. In other words, they are
 * supposed to represent a particular kind of human behavior. In contrast, the
 * SchedulerInterface represents a particular algorithm to schedule the
 * uploading and downloading of pieces.
 * 
 * @author Kees van Reeuwijk
 * 
 */
interface PersonalityInterface {
    /**
     * Returns true iff this peer should stop.
     * 
     * @return <code>true</code> iff the peer should stop.
     */
    boolean shouldStop();

    /** Invoked when this peer is now a seeder. */
    void thisPeerIsSeeder();

    /**
     * Registers that there is a new node in the swarm.
     * 
     * @param peer
     *            The new peer that has arrived.
     */
    void newPeer(IbisIdentifier peer);

    /**
     * Registers that a node of the swarm has disappeared.
     * 
     * @param peer
     *            The peer to remove.
     */
    void removePeer(IbisIdentifier peer);

    /**
     * Registers that a new node is now a seeder.
     * 
     * @param peer
     *            The peer that is now a seeder.
     */
    void peerIsSeeder(IbisIdentifier peer);

    /** Dumps the state of this personality. */
    void dumpState();

    /**
     * Registers that there is a new coordinator peer that we're helping.
     * 
     * @param peer
     *            The peer that we're now helping.
     */
    void newHelpedPeer(IbisIdentifier peer);

    /**
     * Registers that the given peer no longer is helped by us.
     * 
     * @param peer
     *            The peer that is no longer helped.
     * @return <code>true</code> if we knew about the peer in the first place.
     */
    boolean removeHelpedPeer(IbisIdentifier peer);

    /**
     * Prints some statistics to the given print stream.
     * 
     * @param s
     *            The stream to print to.
     */
    void printStatistics(PrintStream s);

    /**
     * Returns the name of this personality.
     * 
     * @return The name.
     */
    String getName();

    /**
     * Registers that the given piece is now available for the peer.
     * 
     * @param piece
     *            The piece that is now available.
     */
    void addedPiece(int piece);
}
