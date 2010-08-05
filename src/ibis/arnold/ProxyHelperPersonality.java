package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.PrintStream;

/**
 * The personality of a helper: wait until the first request for help arrives,
 * and then stick around until all coordinators have stopped.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class ProxyHelperPersonality implements PersonalityInterface {
    private final PeerSet needyPeers = new PeerSet();
    private boolean sawNeedyPeers = false;
    private long firstPieceTime = -1L;
    private long becameSeederTime = -1L;
    private long startTime = -1L;
    private final String policy;
    private final String swarmPersonality;

    ProxyHelperPersonality(String policy, String swarmPersonality) {
        this.policy = policy;
        this.swarmPersonality = swarmPersonality;
        if (Settings.TracePersonalityCreation) {
            Globals.log.reportProgress("Created a proxy helper personality");
        }
    }

    @Override
    public void newPeer(final IbisIdentifier peer) {
        // Not interesting.
    }

    @Override
    public void newHelpedPeer(final IbisIdentifier peer) {
        needyPeers.add(peer);
        sawNeedyPeers = true;
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void peerIsSeeder(final IbisIdentifier peer) {
        if (becameSeederTime < 0) {
            becameSeederTime = System.currentTimeMillis();
            if (startTime >= 0) {
                final long duration = becameSeederTime - startTime;
                System.out.println("Downloaded file in "
                        + Utils.formatSeconds(duration * 1e-3));
                System.out.println("DOWNLOADTIME " + duration + " "
                        + firstPieceTime + " " + policy + " "
                        + swarmPersonality);
                System.out.println("DOWNLOADTIME " + duration);
            }
        }
        // Any coordinator that is now a seeder doesn't need our help any more.
        needyPeers.remove(peer);
    }

    @Override
    public void removePeer(final IbisIdentifier peer) {
        needyPeers.remove(peer);
    }

    @Override
    public boolean shouldStop() {
        return sawNeedyPeers && needyPeers.isEmpty();
    }

    @Override
    public void thisPeerIsSeeder() {
        // Irrelevant
    }

    @Override
    public void dumpState() {
        Globals.log.reportProgress("ProxyHelperPersonality: sawNeedyPeers="
                + sawNeedyPeers + " needyPeers=" + needyPeers);
    }

    @Override
    public boolean removeHelpedPeer(final IbisIdentifier peer) {
        return needyPeers.remove(peer);
    }

    @Override
    public void printStatistics(final PrintStream s) {
        final long now = System.currentTimeMillis();
        if (startTime < 0) {
            s.println("ProxyHelper personality: never became active");
        } else {
            if (becameSeederTime >= 0) {
                s.println("ProxyHelper personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " of which seeding time "
                        + Utils.formatSeconds(1e-3 * (now - becameSeederTime)));
            } else {
                s.println("ProxyHelper personality: service time "
                        + Utils.formatSeconds(1e-3 * (now - startTime))
                        + " with no seeding time");
            }
        }
    }

    @Override
    public String getName() {
        return "ProxyHelper";
    }

    @Override
    public void addedPiece(final int piece) {
        if (firstPieceTime < 0) {
            firstPieceTime = System.currentTimeMillis();
        }
    }

}
