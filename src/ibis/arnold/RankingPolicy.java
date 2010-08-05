package ibis.arnold;

interface RankingPolicy {
    public PeerRanker getPotentialReceivePerformanceRanker();

    public PeerRanker getPeerPerformanceRankerForSeeder();

    public PeerRanker getPeerPerformanceRankerForLeecher();

    public String getName();
}
