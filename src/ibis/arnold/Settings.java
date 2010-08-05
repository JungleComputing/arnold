package ibis.arnold;

class Settings {

    /** Message transmission timeout in ms on essential communications. */
    static final int COMMUNICATION_TIMEOUT = 2000;

    /**
     * The maximal time in ms we wait for the transmitter to shut down.
     */
    static final long TRANSMITTER_SHUTDOWN_TIMEOUT = 30000;

    protected static final int MAXIMAL_SEND_RETRIES = 5;

    /** Maximal number of messages in the receive queue before it blocks. */
    static final int MAXIMAL_RECEIVED_MESSAGE_QUEUE_LENGTH = 50;

    /** The ideal length of the data queue length of the transmitter. */
    static final int IDEAL_TRANSMITTER_QUEUE_LENGTH = 3;

    /** Do we cache connections? */
    static final boolean CACHE_CONNECTIONS = true;

    /** The number of connections we maximally keep open. */
    static final int CONNECTION_CACHE_SIZE = 50;

    /** How many cache accesses unused before the entry is evicted. */
    static final int CONNECTION_CACHE_MAXIMAL_UNUSED_COUNT = 2000;

    // Constants
    static final int CHUNK_SIZE = 1 << 14;

    static final int PIECE_SIZE = CHUNK_SIZE * (1 << 4);

    /** The number of chunk requests we want to have outstanding on each peer. */
    static final int CHUNK_REQUESTS_PER_PEER = 8;

    /** Minimal neighbor set size before we try to get new neighbors. */
    static final int NEIGHBOR_SET_SIZE = 20;

    static final int MAXIMAL_TRACKER_SET_SIZE = 35;

    /** Maximal number of unchoked peers. */
    static final int MAXIMAL_UNCHOKED_PEERS = 8;

    /** The time in ms of the transfer monitor window. */
    static final long TRANSFER_MONITOR_WINDOW_TIME = 800;

    /**
     * Interval in ms between slot updates.
     */
    static final long SLOT_UPDATE_INTERVAL = 3 * TRANSFER_MONITOR_WINDOW_TIME;

    /** The inner block size of the dummy transfered file. */
    static final int DUMMY_INNER_BLOCKSIZE = 10;

    /** The size of a dummy file. */
    protected static final long DUMMY_FILE_SIZE = 1000000000;

    /**
     * The maximal number of downloads for a piece in end-game mode.
     */
    static final int MAXIMAL_ENDGAME_REPLICATION = 2;

    static final int PIECE_REQUESTS_PER_PEER = 2;

    // Debugging flags.

    /**
     * The maximal time in ms the engine can sleep.
     */
    static final int MAXIMAL_ENGINE_SLEEP_INTERVAL = 2500;

    static final boolean TraceCredits = false;

    static final boolean TraceNodeCreation = true;

    static final boolean TracePeers = false;

    static final boolean TraceSends = false;

    static final boolean TraceChunkRequests = false;

    static final boolean TraceScheduler = false;

    static final boolean TraceEngine = false;

    static final boolean TraceTransmitter = false;

    static final boolean TraceReceiver = false;

    static final boolean TracePiecePicking = false;

    static final boolean TraceTransmitterLoop = false;

    static final boolean TracePersonalityCreation = false;

    static final boolean TracePersonalityActions = true;

    static final boolean TracePieceCount = false;

    static final boolean TraceChoking = false;

    static final boolean TraceEndgame = false;

    static final boolean TraceProxyMode = false;

    /** Used for visualization. */
    static final boolean TracePieceTraffic = false;

    static final boolean TraceDetailedProgress = false;

    /**
     * If set, print the final credit level if this peer.
     */
    static final boolean PrintFinalCredit = true;

    /**
     * How much longer will a leecher stay, once it has become a seeder? This is
     * a multiplier for the download time.
     */
    static final double IMPATIENT_SEEDING_FRACTION = 0.5;

    static final int RANKER_MAXIMUM_CHOICES = 30;

    /**
     * The maximal fraction of peers that can have a piece so that it is still
     * profitable for a caching peer to download it.
     */
    static final double MAXIMAL_CACHE_REPLICATION_FRACTION = 0.4;
}
