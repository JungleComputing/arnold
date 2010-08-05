package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;

class GreedyPersonality implements PersonalityInterface {
    private boolean shouldStop = false;
    private long startTime = -1L;
    private long firstPieceTime = -1L;
    private long becameSeederTime = -1L;
    private final String policy;
    private final String swarmPersonality;

    GreedyPersonality(String policy, String swarmPersonality) {
        this.policy = policy;
        this.swarmPersonality = swarmPersonality;
        if (Settings.TracePersonalityCreation) {
            Globals.log.reportProgress("Created a greedy personality");
        }
    }

    @Override
    public void addedPiece(final int piece) {
        if (firstPieceTime < 0) {
            firstPieceTime = System.currentTimeMillis();
        }
    }

    @Override
    public void newPeer(final IbisIdentifier peer) {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void peerIsSeeder(final IbisIdentifier peer) {
        // Not interested.
    }

    @Override
    public void removePeer(final IbisIdentifier peer) {
        // Not interested.
    }

    @Override
    public boolean shouldStop() {
        return shouldStop;
    }

    @Override
    public void thisPeerIsSeeder() {
        becameSeederTime = System.currentTimeMillis();
        final long duration = becameSeederTime - startTime;
        final long firstTime = firstPieceTime - startTime;
        System.out.println("Downloaded file in "
                + Utils.formatSeconds(duration * 1e-3) + "; first piece after "
                + Utils.formatSeconds(firstTime * 1e-3));
        System.out.println("DOWNLOADTIME " + duration + " " + firstTime + " "
                + policy + " " + swarmPersonality);
        shouldStop = true;
    }

    @Override
    public void dumpState() {
        Globals.log.reportProgress("GreedyPersonality: shouldStop="
                + shouldStop);
    }

    @Override
    public void newHelpedPeer(final IbisIdentifier peer) {
        // Not interested.
    }

    @Override
    public boolean removeHelpedPeer(final IbisIdentifier peer) {
        // Not interested.
        return true;
    }

    @Override
    public void printStatistics(final PrintStream s) {
        final long now = System.currentTimeMillis();
        if (startTime < 0) {
            s.println("Greedy personality: never became active");
        } else {
            if (becameSeederTime >= 0) {
                s.println("Greedy personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " of which seeding time "
                        + Utils.formatSeconds(1e-3 * (now - becameSeederTime)));
            } else {
                s.println("Greedy personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " with no seeding time");
            }
        }
    }

    @Override
    public String getName() {
        return "Greedy";
    }

}
