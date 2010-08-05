package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;
import java.util.HashSet;

class TradingPersonality implements PersonalityInterface {
    private final HashSet<IbisIdentifier> needyClients = new HashSet<IbisIdentifier>();
    private boolean sawClients = false;
    private long startTime = -1L;
    private long firstPieceTime = -1l;

    TradingPersonality() {
        if (Settings.TracePersonalityCreation) {
            Globals.log.reportProgress("Created a trading personality");
        }
    }

    @Override
    public void newPeer(final IbisIdentifier peer) {
        needyClients.add(peer);
        if (!sawClients) {
            startTime = System.currentTimeMillis();
        }
        sawClients = true;
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "TradingPersonality: added peer "
                    + peer + " to needy peers");
        }
    }

    @Override
    public void peerIsSeeder(final IbisIdentifier peer) {
        needyClients.remove(peer);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "TradingPersonality: peer " + peer
                    + " is now a seeder, no longer needy, "
                    + needyClients.size() + " peers left");
        }
    }

    @Override
    public void removePeer(final IbisIdentifier peer) {
        needyClients.remove(peer);
        if (Settings.TracePersonalityActions) {
            Globals.log.reportProgress(true, "TradingPersonality: peer " + peer
                    + " has disappeared, no longer needy, "
                    + needyClients.size() + " peers left");
        }
    }

    @Override
    public boolean shouldStop() {
        return sawClients && needyClients.isEmpty();
    }

    @Override
    public void thisPeerIsSeeder() {
        if (Settings.TracePersonalityActions) {
            Globals.log
                    .reportProgress("Trading personality: we are now a seeder");
        }
        final long firstTime = firstPieceTime - startTime;

        if (startTime >= 0) {
            System.out.println("First piece after "
                    + Utils.formatSeconds(firstTime * 1e-3));
        }
    }

    @Override
    public void dumpState() {
        Globals.log.reportProgress("TradingPersonality: sawClients="
                + sawClients + " needyClients=" + needyClients);
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
            s.println("Trading personality: never became active");
        } else {
            if (firstPieceTime >= 0) {
                s
                        .println("Saw first piece after "
                                + Utils
                                        .formatSeconds(1e-3 * (firstPieceTime - startTime)));
            } else {
                s.println("Trading personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime)));
            }
        }
    }

    @Override
    public String getName() {
        return "Trading";
    }

    @Override
    public void addedPiece(final int piece) {
        if (firstPieceTime < 0) {
            firstPieceTime = System.currentTimeMillis();
        }
    }

}
