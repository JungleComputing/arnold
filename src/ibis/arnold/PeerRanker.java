package ibis.arnold;

import java.util.Comparator;

interface PeerRanker extends Comparator<PeerInfo> {
    String getName();

    double getComparisonValue(PeerInfo i);
}
