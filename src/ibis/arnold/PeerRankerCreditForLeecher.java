package ibis.arnold;

/**
 * A class that ranks two peers based on their credit. This variant is intended
 * for use by a leecher.
 */
class PeerRankerCreditForLeecher implements PeerRanker {
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
        return 0;
    }

    @Override
    public String getName() {
        return "Credit ranker";
    }

    @Override
    public double getComparisonValue(final PeerInfo i) {
        double credit = i.getCredit();
        if (i.peerHasChokedUs() || !i.weAreInterested()) {
            credit = Double.MIN_VALUE;
        }
        return credit;
    }
}