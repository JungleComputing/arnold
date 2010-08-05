package ibis.arnold;

class TitForTatRankingPolicy implements RankingPolicy {

    @Override
    public PeerRanker getPotentialReceivePerformanceRanker() {
        return new PeerRankerPotentialReceivePerformance();
    }

    @Override
    public PeerRanker getPeerPerformanceRankerForLeecher() {
        return new PeerRankerReceivePerformance();
    }

    @Override
    public PeerRanker getPeerPerformanceRankerForSeeder() {
        return new PeerRankerSendPerformance();
    }

    @Override
    public String getName() {
        return "TitForTatRankingPolicy";
    }
}
