package ibis.arnold;

/**
 * A peer ranker that always favors peers that registered with this peer
 * earlier. This is obviously a rather arbitrary ranking policy; it is mainly
 * intended as a baseline for the other ranking policies.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class OneTrackRankingPolicy implements RankingPolicy {

    @Override
    public PeerRanker getPotentialReceivePerformanceRanker() {
        return new PeerRankerPotentialReceivePerformance();
    }

    @Override
    public PeerRanker getPeerPerformanceRankerForLeecher() {
        return new PeerRankerSequenceNumber();
    }

    @Override
    public PeerRanker getPeerPerformanceRankerForSeeder() {
        return new PeerRankerSequenceNumber();
    }

    @Override
    public String getName() {
        return "OneTrackRankingPolicy";
    }
}
