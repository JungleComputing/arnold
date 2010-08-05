/**
 * Rank peers according to their receive performance.
 */
package ibis.arnold;

class PeerRankerReceivePerformance implements PeerRanker {
    @Override
    public int compare(final PeerInfo a, final PeerInfo b) {
        final double rateA = getComparisonValue(a);
        final double rateB = getComparisonValue(b);

        if (rateA < rateB) {
            return 1;
        }
        if (rateA > rateB) {
            return -1;
        }
        // Use number of received pieces as tie breaker.
        final int nA = a.getTransferedPieceCount();
        final int nB = b.getTransferedPieceCount();
        if (nA < nB) {
            return 1;
        }
        if (nA > nB) {
            return -1;
        }
        return 0;
    }

    @Override
    public String getName() {
        return "Receive performance";
    }

    @Override
    public double getComparisonValue(final PeerInfo i) {
        double res = i.getReceiveTransferRate();
        if (i.peerHasChokedUs() || !i.weAreInterested()) {
            res = 0;
        }
        return res;
    }

}