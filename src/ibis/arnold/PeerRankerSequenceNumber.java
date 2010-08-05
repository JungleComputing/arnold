/**
 * Rank two peers based on their credit.
 */
package ibis.arnold;

class PeerRankerSequenceNumber implements PeerRanker {
    @Override
    public int compare(final PeerInfo a, final PeerInfo b) {
        final int rateA = a.sequenceNumber;
        final int rateB = b.sequenceNumber;

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
        return "Sequence-number ranker";
    }

    @Override
    public double getComparisonValue(final PeerInfo i) {
        return i.sequenceNumber;
    }
}