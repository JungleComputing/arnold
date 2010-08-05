package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;

/**
 * A composite personality: become an Altruist if you start as a seeder, else
 * become a Greedy.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class BigSwarmPersonality implements PersonalityInterface {
    private PersonalityInterface personality = null;
    private final String policy;
    private static final String swarmPersonality = "TFT";

    BigSwarmPersonality(final String policy) {
        this.policy = policy;
        if (Settings.TracePersonalityCreation) {
            Globals.log.reportProgress("Created a big swarm personality");
        }
    }

    @Override
    public void newPeer(final IbisIdentifier peer) {
        if (personality == null) {
            personality = new GreedyPersonality(policy, swarmPersonality);
        }
        personality.newPeer(peer);
    }

    @Override
    public void peerIsSeeder(final IbisIdentifier peer) {
        if (personality == null) {
            personality = new GreedyPersonality(policy, swarmPersonality);
        }
        personality.peerIsSeeder(peer);
    }

    @Override
    public void removePeer(final IbisIdentifier peer) {
        if (personality == null) {
            personality = new GreedyPersonality(policy, swarmPersonality);
        }
        personality.removePeer(peer);
    }

    @Override
    public boolean shouldStop() {
        if (personality == null) {
            return false;
        }
        return personality.shouldStop();
    }

    @Override
    public void thisPeerIsSeeder() {
        if (personality == null) {
            personality = new AltruisticPersonality(policy, swarmPersonality);
        }
        personality.thisPeerIsSeeder();
    }

    @Override
    public void dumpState() {
        if (personality != null) {
            Globals.log
                    .reportProgress("BigSwarmPersonality: nested personality is "
                            + personality.getClass());
            personality.dumpState();
        } else {
            Globals.log
                    .reportInternalError("BigSwarmPersonality: no personality has been chosen");
        }
    }

    @Override
    public void newHelpedPeer(final IbisIdentifier peer) {
        if (personality != null) {
            personality.newHelpedPeer(peer);
        } else {
            Globals.log
                    .reportInternalError("BigSwarmPersonality: no personality has been chosen");
        }
    }

    @Override
    public boolean removeHelpedPeer(final IbisIdentifier peer) {
        if (personality != null) {
            return personality.removeHelpedPeer(peer);
        }
        Globals.log
                .reportInternalError("BigSwarmPersonality: no personality has been chosen");
        return true;
    }

    @Override
    public void printStatistics(final PrintStream s) {
        if (personality != null) {
            personality.printStatistics(s);
        }
    }

    @Override
    public String getName() {
        if (personality == null) {
            return "BigSwarm (<undecided>)";
        }
        return "BigSwarm (" + personality.getName() + ")";
    }

    @Override
    public void addedPiece(final int piece) {
        if (personality == null) {
            personality = new GreedyPersonality(policy, swarmPersonality);
        }
        personality.addedPiece(piece);
    }

}
