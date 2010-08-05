package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.HashSet;

/**
 * A model of user behavior, where the peer remains in the swarm until all
 * seeders have become leechers.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class AltruisticPersonality implements PersonalityInterface {
    private final HashSet<IbisIdentifier> needyClients = new HashSet<IbisIdentifier>();
    private boolean sawClients = false;
    private boolean haveMyStuff = false;
    private long startTime = -1L;
    private long firstPieceTime = -1l;
    private long becameSeederTime = -1L;
    private final String swarmPolicy;
    private final String personality;

    AltruisticPersonality(final String swarmPolicy, final String personality) {
        this.swarmPolicy = swarmPolicy;
        this.personality = personality;
        if (Settings.TracePersonalityCreation) {
            Globals.log.reportProgress("Created an altruistic personality");
        }
    }

    @Override
    public void newPeer(final IbisIdentifier peer) {
        needyClients.add(peer);
        if (!sawClients && !haveMyStuff) {
            startTime = System.currentTimeMillis();
        }
        sawClients = true;
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true,
                    "AltruisticPersonality: added peer " + peer
                            + " to needy peers");
        }
    }

    @Override
    public void peerIsSeeder(final IbisIdentifier peer) {
        needyClients.remove(peer);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "AltruisticPersonality: peer "
                    + peer + " is now a seeder, no longer needy, "
                    + needyClients.size() + " peers left");
        }
    }

    @Override
    public void removePeer(final IbisIdentifier peer) {
        needyClients.remove(peer);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "AltruisticPersonality: peer "
                    + peer + " has disappeared, no longer needy, "
                    + needyClients.size() + " peers left");
        }
    }

    @Override
    public boolean shouldStop() {
        return sawClients && haveMyStuff && needyClients.isEmpty();
    }

    @Override
    public void thisPeerIsSeeder() {
        haveMyStuff = true;
        if (Settings.TracePersonalityActions) {
            Globals.log
                    .reportProgress("Altruistic personality: we are now a seeder");
        }
        if (becameSeederTime < 0) {
            becameSeederTime = System.currentTimeMillis();
            final long firstTime = firstPieceTime - startTime;

            if (startTime >= 0) {
                final long duration = becameSeederTime - startTime;
                System.out.println("Downloaded file in "
                        + Utils.formatSeconds(duration * 1e-3)
                        + "; first piece after "
                        + Utils.formatSeconds(firstTime * 1e-3));
                System.out.println("DOWNLOADTIME " + duration + " " + firstTime
                        + " " + swarmPolicy + " " + personality);
            }
        }
    }

    @Override
    public void dumpState() {
        Globals.log.reportProgress("AltruisticPersonality: sawClients="
                + sawClients + " haveMyStuff=" + haveMyStuff + " needyClients="
                + needyClients);
    }

    @Override
    public void newHelpedPeer(final IbisIdentifier peer) {
        // Not interested.
    }

    @Override
    public boolean removeHelpedPeer(final IbisIdentifier peer) {
        return true;
    }

    @Override
    public void printStatistics(final PrintStream s) {
        final long now = System.currentTimeMillis();
        if (startTime < 0) {
            s.println("Altruistic personality: never became active");
        } else {
            if (firstPieceTime >= 0) {
                s
                        .println("Saw first piece after "
                                + Utils
                                        .formatSeconds(1e-3 * (firstPieceTime - startTime)));
            }
            if (becameSeederTime >= 0) {
                s.println("Altruistic personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " of which seeding time "
                        + Utils.formatSeconds(1e-3 * (now - becameSeederTime)));
            } else {
                s.println("Altruistic personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " with no seeding time");
            }
        }
    }

    @Override
    public String getName() {
        return "Altruistic";
    }

    @Override
    public void addedPiece(final int piece) {
        if (firstPieceTime < 0) {
            firstPieceTime = System.currentTimeMillis();
        }
    }

}
