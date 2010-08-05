/**
 * 
 */
package ibis.arnold;

class PeerRankerSendPerformance implements PeerRanker {
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
        return "Send performance";
    }

    @Override
    public double getComparisonValue(final PeerInfo i) {
        double res = i.getSendTransferRate();
        if (!i.peerIsInterested()) {
            res = 0;
        }
        return res;
    }
}