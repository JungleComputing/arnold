package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.HashSet;

class ImpatientPersonality implements PersonalityInterface {
    private final HashSet<IbisIdentifier> needyClients = new HashSet<IbisIdentifier>();
    private boolean sawClients = false;
    private boolean haveMyStuff = false;
    private long startTime = -1L;
    private long firstPieceTime = -1l;
    private long becameSeederTime = -1L;
    private final String policy;
    private final String personality;
    private final double lingerFraction;
    private long stopTime = -1L;

    ImpatientPersonality(final double lingerFraction, final String policy,
            final String personality) {
        this.lingerFraction = lingerFraction;
        this.policy = policy;
        this.personality = personality;
        if (Settings.TracePersonalityCreation) {
            Globals.log.reportProgress("Created an impatient personality");
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
                    "ImpatientPersonality: added peer " + peer
                            + " to needy peers");
        }
    }

    @Override
    public void peerIsSeeder(final IbisIdentifier peer) {
        needyClients.remove(peer);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "ImpatientPersonality: peer "
                    + peer + " is now a seeder, no longer needy");
        }
    }

    @Override
    public void removePeer(final IbisIdentifier peer) {
        needyClients.remove(peer);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "ImpatientPersonality: peer "
                    + peer + " has disappeared, no longer needy");
        }
    }

    @Override
    public boolean shouldStop() {
        final boolean passedDeadline = stopTime >= 0
                && stopTime < System.currentTimeMillis();
        final boolean noNeedyClients = sawClients && needyClients.isEmpty();
        final boolean res = haveMyStuff && (noNeedyClients || passedDeadline);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress("Altruist: sawClients=" + sawClients
                    + " haveMyStuff=" + haveMyStuff + " needyclients="
                    + needyClients.size() + " shouldStop()=" + res);
        }
        return res;
    }

    @Override
    public void thisPeerIsSeeder() {
        haveMyStuff = true;
        if (becameSeederTime < 0) {
            becameSeederTime = System.currentTimeMillis();
            final long firstTime = firstPieceTime - startTime;

            if (startTime >= 0) {
                final long duration = becameSeederTime - startTime;
                stopTime = becameSeederTime
                        + (long) (lingerFraction * duration);
                System.out.println("Downloaded file in "
                        + Utils.formatSeconds(duration * 1e-3)
                        + "; first piece after "
                        + Utils.formatSeconds(firstTime * 1e-3));
                System.out.println("DOWNLOADTIME " + duration + " " + firstTime
                        + " " + policy + " " + personality);
            }
        }
    }

    @Override
    public void dumpState() {
        final String stopMoment = stopTime < 0 ? "(unknown)" : Utils
                .formatSeconds(1e-3 * (stopTime - startTime));
        Globals.log.reportProgress("ImpatientPersonality: sawClients="
                + sawClients + " haveMyStuff=" + haveMyStuff + " needyClients="
                + needyClients + " stopMoment=" + stopMoment);
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
            s.println("Impatient personality: never became active");
        } else {
            if (firstPieceTime >= 0) {
                s
                        .println("Saw first piece after "
                                + Utils
                                        .formatSeconds(1e-3 * (firstPieceTime - startTime)));
            }
            if (becameSeederTime >= 0) {
                s.println("Impatient personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " of which seeding time "
                        + Utils.formatSeconds(1e-3 * (now - becameSeederTime)));
            } else {
                s.println("Impatient personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " with no seeding time");
            }
        }
    }

    @Override
    public String getName() {
        return "Impatient";
    }

    @Override
    public void addedPiece(final int piece) {
        if (firstPieceTime < 0) {
            firstPieceTime = System.currentTimeMillis();
        }
    }

}
