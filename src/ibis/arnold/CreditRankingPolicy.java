package ibis.arnold;

class CreditRankingPolicy implements RankingPolicy {

    @Override
    public PeerRanker getPotentialReceivePerformanceRanker() {
        return new PeerRankerPotentialReceivePerformance();
    }

    @Override
    public PeerRanker getPeerPerformanceRankerForLeecher() {
        return new PeerRankerCreditForLeecher();
    }

    @Override
    public PeerRanker getPeerPerformanceRankerForSeeder() {
        return new PeerRankerCreditForLeecher();
    }

    @Override
    public String getName() {
        return "CreditRankingPolicy";
    }
}
