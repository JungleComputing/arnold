package ibis.arnold;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The BitTorrent engine.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class Engine extends Thread implements PacketReceiveListener,
        RegistryEventHandler, EngineInterface {
    private static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.MEMBERSHIP_UNRELIABLE,
            IbisCapabilities.ELECTIONS_STRICT);
    private static final String SPECIAL_PEER_ELECTION_NAME = "special-peer-election";
    private static final String SEEDER_ELECTION_NAME = "seeder-election";
    private final SharedFileInterface sharedFile;
    private final ReceivedMessageQueue receivedMessageQueue = new ReceivedMessageQueue(
            Settings.MAXIMAL_RECEIVED_MESSAGE_QUEUE_LENGTH);
    private final TimeStatistics receivedMessageQueueStatistics = new TimeStatistics();
    private final IncompletePiecesList outstandingRequests = new IncompletePiecesList();
    private final Flag stopped = new Flag(false);
    private final Transmitter transmitter;
    private final SchedulerInterface scheduler;
    private final ConcurrentLinkedQueue<IbisIdentifier> deletedPeers = new ConcurrentLinkedQueue<IbisIdentifier>();
    private final ConcurrentLinkedQueue<IbisIdentifier> newPeers = new ConcurrentLinkedQueue<IbisIdentifier>();
    private final PacketUpcallReceivePort receivePort;
    private final Credit credit = new Credit();
    final Ibis localIbis;
    private int activePeers = 0;
    private PersonalityInterface personality;
    private long receivedMessageHandlingTime = 0;
    private long requestsHandlingTime = 0;
    private long idleTime = 0;
    private long sendQueueHandlingTime;
    private final boolean isSpecialPeer;
    private final WatchdogTimer watchdog = new WatchdogTimer(10000);

    Engine(final SharedFileInterface sharedFile, final boolean proxymode,
            final boolean helper, final boolean altruisticHelpers,
            final boolean altruisticLeechers, final boolean impatientLeechers)
            throws IOException, IbisCreationFailedException {
        super("Arnold engine thread");
        boolean runForSpecialNode = true;
        this.sharedFile = sharedFile;
        RankingPolicy rankingPolicy;
        boolean useSlots = false;
        if (Utils.getExistenceProperty("arnold.credit-based")) {
            rankingPolicy = new CreditRankingPolicy();
            useSlots = false;
        } else if (Utils.getExistenceProperty("arnold.one-track-based")) {
            rankingPolicy = new OneTrackRankingPolicy();
        } else {
            rankingPolicy = new TitForTatRankingPolicy();
        }
        final PersonalityInterface thePersonality;
        if (helper) {
            runForSpecialNode = false;
            if (altruisticHelpers) {
                thePersonality = new AltruisticPersonality(
                        rankingPolicy.getName(), "altruistic");
            } else {
                thePersonality = new ProxyHelperPersonality(
                        rankingPolicy.getName(), "TFT");
            }
        } else {
            if (altruisticLeechers) {
                thePersonality = new AltruisticPersonality(
                        rankingPolicy.getName(), "altruistic");
            } else if (impatientLeechers) {
                thePersonality = new ImpatientPersonality(
                        Settings.IMPATIENT_SEEDING_FRACTION,
                        rankingPolicy.getName(), "impatient");
            } else {
                thePersonality = new BigSwarmPersonality(
                        rankingPolicy.getName());
            }
        }
        this.personality = thePersonality;
        this.transmitter = new Transmitter(this);
        SchedulerInterface theScheduler = null;
        final Properties ibisProperties = new Properties();
        this.localIbis = IbisFactory.createIbis(ibisCapabilities,
                ibisProperties, true, this, PacketSendPort.portType,
                PacketUpcallReceivePort.portType);
        final Registry registry = localIbis.registry();
        final IbisIdentifier myIbis = localIbis.identifier();
        if (sharedFile.canSetValid()) {
            // Elect seeders
            final int n = Utils.getIntProperty("arnold.seeder-count", 1);
            for (int i = 0; i < n; i++) {
                final IbisIdentifier m = registry.elect(SEEDER_ELECTION_NAME
                        + i);
                if (m.equals(myIbis)) {
                    sharedFile.setValid();
                    if (!sharedFile.isComplete()) {
                        Globals.log
                                .reportInternalError("Seeder doesn't have all pieces???");
                    }
                    thePersonality.thisPeerIsSeeder();
                    runForSpecialNode = false;
                    break; // Don't join subsequent elections.
                }
            }
        }
        final int numberOfPieces = this.sharedFile.getNumberOfPieces();
        final PieceSet knownPieces = this.sharedFile.getKnownPieces();
        final PieceSet notKnownPieces = knownPieces.getInverse();
        if (runForSpecialNode) {
            final IbisIdentifier m = registry.elect(SPECIAL_PEER_ELECTION_NAME);
            isSpecialPeer = m.equals(myIbis);
        } else {
            isSpecialPeer = false;
        }
        if (isSpecialPeer) {
            final String pnm = "arnold.special-start-credit";
            final double specialBudget = Utils.getDoubleProperty(pnm, 0.0);
            credit.add(specialBudget, "special peer gets special budget");
            final boolean cachingMode = Utils
                    .getExistenceProperty("arnold.caching-peer");
            if (cachingMode) {
                theScheduler = new CacheScheduler(this, transmitter,
                        knownPieces, notKnownPieces, numberOfPieces);
                this.personality = new TradingPersonality();
            }
        }
        if (theScheduler == null) {
            if (proxymode && !helper) {
                final SchedulerInterface helpersScheduler = buildPlainScheduler(
                        knownPieces, numberOfPieces, notKnownPieces,
                        rankingPolicy, useSlots);
                theScheduler = new ProxyCoordinatorScheduler(helpersScheduler,
                        transmitter, numberOfPieces, knownPieces);
            } else if (helper) {
                final SchedulerInterface coordinatorsScheduler = buildPlainScheduler(
                        knownPieces, numberOfPieces, notKnownPieces,
                        rankingPolicy, useSlots);
                final SchedulerInterface publicScheduler = buildPlainScheduler(
                        knownPieces, numberOfPieces, notKnownPieces,
                        rankingPolicy, useSlots);
                theScheduler = new ProxyHelperScheduler(publicScheduler,
                        coordinatorsScheduler, transmitter);
            } else {
                theScheduler = buildPlainScheduler(knownPieces, numberOfPieces,
                        notKnownPieces, rankingPolicy, useSlots);
            }
        }
        receivePort = new PacketUpcallReceivePort(localIbis,
                Globals.receivePortName, this);
        System.out.println("runForSpecialNode=" + runForSpecialNode);
        this.scheduler = theScheduler;
        transmitter.start();
        registry.enableEvents();
        receivePort.enable();
        if (Settings.TraceNodeCreation) {
            Globals.log.reportProgress("Created ibis " + myIbis + " "
                    + Utils.getPlatformVersion() + " host "
                    + Utils.getCanonicalHostname());
            Globals.log
                    .reportProgress("scheduler=" + scheduler.getName()
                            + " personality=" + personality.getName()
                            + " rankingPolicy=" + rankingPolicy.getName()
                            + " seeder=" + sharedFile.isComplete()
                            + " specialPeer=" + isSpecialPeer);
            Utils.showSystemProperties(Globals.log.getPrintStream(), "arnold.");
        }
        watchdog.start();
    }

    private SchedulerInterface buildPlainScheduler(final PieceSet knownPieces,
            final int numberOfPieces, final PieceSet notKnownPieces,
            final RankingPolicy rankingPolicy, final boolean useSlots) {
        final SchedulerInterface res;
        if (useSlots) {
            res = new BitTorrentScheduler(this, transmitter, knownPieces,
                    notKnownPieces, rankingPolicy, numberOfPieces);
        } else {
            res = new GreedyScheduler(this, transmitter, knownPieces,
                    notKnownPieces, numberOfPieces);
        }
        return res;
    }

    @Override
    public void died(final IbisIdentifier peer) {
        if (peer.equals(localIbis.identifier())) {
            if (!stopped.isSet()) {
                Globals.log
                        .reportError("This peer has been declared dead, we might as well stop");
                setStopped();
            }
        } else {
            transmitter.deletePeer(peer);
            deletedPeers.add(peer);
        }
        wakeEngineThread(); // Something interesting has happened.
    }

    @Override
    public void left(final IbisIdentifier peer) {
        if (peer.equals(localIbis.identifier())) {
            if (!stopped.isSet()) {
                Globals.log
                        .reportError("This peer has been declared `left', we might as well stop");
                setStopped();
            }
        } else {
            transmitter.deletePeer(peer);
            deletedPeers.add(peer);
        }
        wakeEngineThread(); // Something interesting has happened.
    }

    @Override
    public void electionResult(final String arg0, final IbisIdentifier arg1) {
        // Not interesting.
    }

    @Override
    public void gotSignal(final String arg0, final IbisIdentifier arg1) {
        // Not interesting.
    }

    @Override
    public void joined(final IbisIdentifier peer) {
        newPeers.add(peer);
        if (Settings.TraceEngine) {
            Globals.log.reportProgress("New peer " + peer);
        }
        wakeEngineThread(); // Something interesting has happened.
    }

    private void registerNewPeer(final IbisIdentifier peer) {
        if (peer.equals(localIbis.identifier())) {
            // That's the local node. Ignore.
            return;
        }
        activePeers++;
        if (Settings.TracePeers) {
            Globals.log.reportProgress("Peer " + peer + " has joined");
        }
        scheduler.addPeer(peer, false, personality);
    }

    private void registerPeerLeft(final IbisIdentifier peer) {
        if (Settings.TracePeers) {
            Globals.log.reportProgress("Peer " + peer + " has left");
        }
        activePeers--;
        outstandingRequests.removePeer(peer, scheduler);
        scheduler.removePeer(peer);
        personality.removePeer(peer);
    }

    private boolean registerNewAndDeletedPeers() {
        boolean changes = false;
        while (true) {
            final IbisIdentifier peer = deletedPeers.poll();
            if (peer == null) {
                break;
            }
            registerPeerLeft(peer);
            changes = true;
        }
        while (true) {
            final IbisIdentifier peer = newPeers.poll();
            if (peer == null) {
                break;
            }
            registerNewPeer(peer);
            changes = true;
        }
        return changes;
    }

    @Override
    public void poolClosed() {
        // Not interesting.
    }

    @Override
    public void poolTerminated(final IbisIdentifier arg0) {
        setStopped();
    }

    /**
     * This ibis was reported as 'may be dead'. Try not to communicate with it.
     * 
     * @param peer
     *            The peer that may be dead.
     */
    @Override
    public void setSuspect(final IbisIdentifier peer) {
        if (Settings.TracePeers) {
            Globals.log.reportProgress(true, "Peer " + peer + " may be dead");
        }
        try {
            localIbis.registry().assumeDead(peer);
        } catch (final IOException e) {
            // Nothing we can do about it.
        }
    }

    private void setStopped() {
        stopped.set();
        wakeEngineThread(); // Something interesting has happened.
    }

    @Override
    public void cancelPieceDownload(final IbisIdentifier peer, final int piece) {
        if (Settings.TracePieceTraffic) {
            final PrintStream s = Globals.log.getPrintStream();
            s.println("CANCELEDDOWNLOAD\t" + System.currentTimeMillis() + "\t"
                    + piece + "\t" + peer + "\t" + localIbis.identifier());
        }
        outstandingRequests.cancelPieceDownload(peer, piece, transmitter);
    }

    @Override
    public void startPieceDownload(final IbisIdentifier peer, final int piece) {
        if (Settings.TracePieceTraffic) {
            final PrintStream s = Globals.log.getPrintStream();
            s.println("STARTDOWNLOAD\t" + System.currentTimeMillis() + "\t"
                    + piece + "\t" + peer + "\t" + localIbis.identifier());
        }
        outstandingRequests.add(peer, piece, sharedFile.getPieceSize(piece));
    }

    /**
     * Handles an incoming message.
     * 
     * @param packet
     *            The message to handle.
     */
    @Override
    public void messageReceived(final Message packet) {
        // We are not allowed to do I/O in this thread, and we shouldn't
        // take too much time, so put all messages in a local queue to be
        // handled by the main loop.
        packet.arrivalTime = System.currentTimeMillis();
        receivedMessageQueue.add(packet);
        if (Settings.TraceReceiver) {
            Globals.log.reportProgress("Added to receive queue: " + packet);
        }
        wakeEngineThread(); // Something interesting has happened.
    }

    /** Tell the engine thread that something interesting has happened. */
    @Override
    public void wakeEngineThread() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * Extracts all information from an incoming message, and update the
     * administration.
     * 
     * @param msg
     *            The incoming message.
     */
    private void handleMessage(final Message msg) {
        if (msg instanceof RequestMessage) {
            final RequestMessage r = (RequestMessage) msg;
            scheduler.addChunkRequest(r.source, r.chunk);
            scheduler.updateCredit(r.source, r.credit);
        } else if (msg instanceof PieceMessage) {
            handlePieceMessage((PieceMessage) msg);
        } else if (msg instanceof RequestPiecesMessage) {
            handleRequestPiecesMessage((RequestPiecesMessage) msg);
        } else if (msg instanceof ChokedMessage) {
            handleChokeMessage((ChokedMessage) msg);
        } else if (msg instanceof InterestedMessage) {
            handleInterestedMessage((InterestedMessage) msg);
        } else if (msg instanceof BitSetMessage) {
            handleBitSetMessage((BitSetMessage) msg);
        } else if (msg instanceof CancelMessage) {
            final CancelMessage c = (CancelMessage) msg;
            scheduler.removeChunkRequest(c.source, c.chunk);
        } else if (msg instanceof HaveMessage) {
            handleHaveMessage((HaveMessage) msg);
        } else if (msg instanceof AskForHelpMessage) {
            handleAskForHelpMessage((AskForHelpMessage) msg);
        } else if (msg instanceof StopHelpingMessage) {
            handleStopHelpingMessage((StopHelpingMessage) msg);
        } else if (msg instanceof JoinHelpersMessage) {
            handleJoinHelpersMessage((JoinHelpersMessage) msg);
        } else if (msg instanceof ResignAsHelperMessage) {
            handleResignAsHelperMessage((ResignAsHelperMessage) msg);
        } else if (msg instanceof CloseConnectionMessage) {
            handleCloseConnectionMessage((CloseConnectionMessage) msg);
        } else {
            Globals.log.reportInternalError("Don't know how to handle a "
                    + msg.getClass() + " message");
        }
    }

    private void handleInterestedMessage(final InterestedMessage m) {
        scheduler.setPeerIsInterested(m.source, m.flag);
    }

    private void handleChokeMessage(final ChokedMessage m) {
        scheduler.setPeerHasChokedUs(m.source, m.flag);
    }

    private void handleAskForHelpMessage(final AskForHelpMessage m) {
        final boolean accepted = scheduler.askForHelp(m.source, personality);
        if (accepted) {
            personality.newHelpedPeer(m.source);
        }
    }

    private void handleStopHelpingMessage(final StopHelpingMessage m) {
        final IbisIdentifier peer = m.source;
        scheduler.handleStoppedHelping(peer);
        if (!personality.removeHelpedPeer(peer)) {
            Globals.log
                    .reportInternalError("STOP_HELPING message from unknown peer "
                            + peer);
        }
    }

    private void handleJoinHelpersMessage(final JoinHelpersMessage m) {
        scheduler.peerHasJoinedAsProxyHelper(m.source, personality);
    }

    private void handleResignAsHelperMessage(final ResignAsHelperMessage m) {
        scheduler.peerHasResignedAsProxyHelper(m.source);
    }

    private void handleCloseConnectionMessage(final CloseConnectionMessage m) {
        scheduler.handleClosedConnection(m.source);
    }

    private void handleHaveMessage(final HaveMessage packet) {
        scheduler
                .registerPeerHasPiece(packet.source, packet.piece, personality);
    }

    private void handleBitSetMessage(final BitSetMessage msg) {
        scheduler.setPeerHasPieces(msg.source, msg.bits, personality);
    }

    private void handleRequestPiecesMessage(final RequestPiecesMessage msg) {
        scheduler.requestPieces(msg.bits);
    }

    private void handlePieceMessage(final PieceMessage msg) {
        final int piece = msg.piece;
        if (sharedFile.isValidPiece(piece)) {
            // Somebody sent us data for a piece we already have.
            // Ignore it.
            return;
        }
        scheduler.updateCredit(msg.source, msg.credit);
        scheduler.registerReceivedChunk(msg.source, msg.data.length);
        final byte completedPiece[] = outstandingRequests.updatePiece(
                msg.source, piece, msg.offset, msg.data);
        if (completedPiece != null) {
            // That completed our piece.
            boolean valid;
            try {
                valid = sharedFile.storePiece(piece, completedPiece);
            } catch (final IOException e) {
                Globals.log.reportError("Failed to write piece " + piece + ": "
                        + e.getLocalizedMessage());
                e.printStackTrace();
                throw new DownloadFailedError("Failed to write piece " + piece,
                        e);
            }
            if (valid) {
                outstandingRequests.cancelPieceDownload(piece, transmitter);
                // Deduct the bytes we got from this piece from our account.
                credit.add(-sharedFile.getPieceSize(piece),
                        "received piece from " + msg.source);
                scheduler.registerCompletedPiece(msg.source, piece);
                personality.addedPiece(piece);
                if (sharedFile.isComplete()) {
                    personality.thisPeerIsSeeder();
                }
                if (Settings.TracePieceTraffic) {
                    final PrintStream s = Globals.log.getPrintStream();
                    s.println("COMPLETEDDOWNLOAD\t"
                            + System.currentTimeMillis() + "\t" + piece + "\t"
                            + msg.source + "\t" + localIbis.identifier());
                }
            } else {
                scheduler.registerIncorrectPiece(msg.source, piece);
                if (Settings.TracePieceTraffic) {
                    final PrintStream s = Globals.log.getPrintStream();
                    s.println("FAILEDDOWNLOAD\t" + System.currentTimeMillis()
                            + "\t" + piece + "\t" + msg.source + "\t"
                            + localIbis.identifier());
                }
            }
        }
    }

    private boolean handleIncomingMessages() {
        final long start = System.nanoTime();
        boolean progress = false;
        while (true) {
            final Message msg = receivedMessageQueue.getNext();
            if (msg == null) {
                break;
            }
            final long lingerTime = System.currentTimeMillis()
                    - msg.arrivalTime;
            receivedMessageQueueStatistics.registerSample(lingerTime * 1e-3);
            handleMessage(msg);
            progress = true;
        }
        final long duration = System.nanoTime() - start;
        receivedMessageHandlingTime += duration;
        return progress;
    }

    /**
     * Fulfil the request for one chunk.
     * 
     * @return <code>true</code> iff we actually fulfilled a chunk request.
     */
    private boolean fulfilChunkRequest() {
        final ChunkRequest request = scheduler.getNextChunkRequest();

        if (request == null) {
            // No requests.
            return false;
        }
        final Chunk chunk = request.chunk;
        final byte data[];
        try {
            data = sharedFile.readChunk(chunk);
        } catch (final IOException e) {
            Globals.log.reportError("Cannot read chunk " + chunk
                    + " from shared file");
            e.printStackTrace();
            return false;
        }
        credit.add(chunk.size, "sent chunk to " + request.peer);
        final Message piece = new PieceMessage(credit.getValue(), chunk.piece,
                chunk.offset, data);
        transmitter.addToDataQueue(request.peer, piece);
        return true;
    }

    private boolean thereAreRequestsToFulfill() {
        return transmitter.needsMoreData()
                && scheduler.haveIncomingChunkRequests();
    }

    private boolean keepSendQueueFilled() {
        final long start = System.nanoTime();
        boolean progress = false;
        if (transmitter.needsMoreData()) {
            progress = fulfilChunkRequest();
            if (!progress) {
                progress = scheduler.generateMoreTransmission();
            }
        }
        final long duration = System.nanoTime() - start;
        sendQueueHandlingTime += duration;
        return progress;
    }

    /**
     * Make sure there are enough outstanding requests.
     */
    private boolean maintainOutstandingRequests() {
        final long start = System.nanoTime();
        final boolean progress = outstandingRequests.maintainRequests(credit,
                transmitter);
        final long duration = System.nanoTime() - start;
        requestsHandlingTime += duration;
        return progress;
    }

    private synchronized void printStatistics(final PrintStream s) {
        s.println("message handling "
                + Utils.formatSeconds(1e-9 * receivedMessageHandlingTime)
                + " requests handling "
                + Utils.formatSeconds(1e-9 * requestsHandlingTime)
                + " send queue handling "
                + Utils.formatSeconds(1e-9 * sendQueueHandlingTime) + " idle "
                + Utils.formatSeconds(1e-3 * idleTime));
        receivedMessageQueueStatistics.printStatistics(s,
                "receive queue linger time");
    }

    private synchronized void dumpEngineState() {
        Globals.log.reportProgress("Main thread was only woken by timeout");
        receivedMessageQueue.dump();
        receivedMessageQueueStatistics.printStatistics(
                Globals.log.getPrintStream(), "receive queue linger time");
        Globals.log.reportProgress("Maximal receive queue length: "
                + receivedMessageQueue.getMaximalQueueLength());
        Globals.log.reportProgress("message handling "
                + Utils.formatSeconds(1e-9 * receivedMessageHandlingTime)
                + " requests handling "
                + Utils.formatSeconds(1e-9 * requestsHandlingTime)
                + " send queue handling "
                + Utils.formatSeconds(1e-9 * sendQueueHandlingTime) + " idle "
                + Utils.formatSeconds(1e-3 * idleTime));
        Globals.log.reportProgress("Engine: " + activePeers + " active, "
                + deletedPeers.size() + " deleted peers");
        outstandingRequests.dumpState();
        transmitter.dumpState();
        scheduler.dumpState();
        personality.dumpState();
    }

    @Override
    public void run() {
        final int sleepTime = Settings.MAXIMAL_ENGINE_SLEEP_INTERVAL;
        try {
            while (!stopped.isSet()) {
                boolean sleptLong = false;
                boolean progress;
                do {
                    final boolean progressIncoming = handleIncomingMessages();
                    final boolean progressPeerChurn = registerNewAndDeletedPeers();
                    final boolean progressRequests = maintainOutstandingRequests();
                    final boolean progressSendQueue = keepSendQueueFilled();
                    progress = progressIncoming | progressPeerChurn
                            | progressRequests | progressSendQueue;
                    if (Settings.TraceDetailedProgress) {
                        // if (sharedFile.isComplete()) {
                        if (progress) {
                            Globals.log.reportProgress("EE p=true i="
                                    + progressIncoming + " c="
                                    + progressPeerChurn + " r="
                                    + progressRequests + " s="
                                    + progressSendQueue);
                            if (progressIncoming) {
                                receivedMessageQueue.printCounts();
                            }
                            if (progressRequests) {
                                outstandingRequests.dumpState();
                            }
                            if (progressSendQueue) {
                                Globals.log.reportProgress("  credit="
                                        + credit.getValue().toString());
                            }
                        }
                    }
                } while (progress);
                watchdog.reset();
                synchronized (this) {
                    final boolean messageQueueIsEmpty = receivedMessageQueue
                            .isEmpty();
                    final boolean noRequestsToFulfill = !thereAreRequestsToFulfill();
                    final boolean noRequestsToSubmit = !outstandingRequests
                            .requestsToSubmit();
                    if (!stopped.isSet() && messageQueueIsEmpty
                            && newPeers.isEmpty() && deletedPeers.isEmpty()
                            && noRequestsToFulfill && noRequestsToSubmit) {
                        try {
                            final long sleepStartTime = System
                                    .currentTimeMillis();
                            if (Settings.TraceEngine) {
                                Globals.log
                                        .reportProgress("Main loop: waiting");
                            }
                            this.wait(sleepTime);
                            final long sleepInterval = System
                                    .currentTimeMillis() - sleepStartTime;
                            idleTime += sleepInterval;
                            sleptLong = sleepInterval > 9 * sleepTime / 10;
                        } catch (final InterruptedException e) {
                            // Ignored.
                        }
                    }
                }
                if (sleptLong) {
                    if (activePeers > 0) {
                        dumpEngineState();
                    }
                }
                if (personality.shouldStop()) {
                    if (Settings.TracePersonalityActions) {
                        Globals.log.reportProgress("Personality "
                                + personality.getName()
                                + " says we should stop");
                        personality.dumpState();
                    }
                    stopped.set();
                }
            }
        } finally {
            transmitter.setShuttingDown();
            scheduler.shutdown();
            watchdog.setStopped();
            try {
                transmitter.join(Settings.TRANSMITTER_SHUTDOWN_TIMEOUT);
            } catch (final InterruptedException e) {
                // ignore.
            }
            transmitter.setStopped();
            try {
                sharedFile.close();
            } catch (final IOException e) {
                Globals.log.reportError("Cannot close shared file '"
                        + sharedFile + "': " + e.getLocalizedMessage());
                throw new DownloadFailedError("Cannot close shared file '"
                        + sharedFile + "'", e);
            }
            try {
                localIbis.end();
            } catch (final IOException x) {
                // Nothing we can do about it.
            }
        }
        printStatistics(Globals.log.getPrintStream());
        scheduler.printStatistics(Globals.log.getPrintStream());
        personality.printStatistics(Globals.log.getPrintStream());
        transmitter.printStatistics(Globals.log.getPrintStream());
        if (Settings.PrintFinalCredit) {
            System.out.println("FINALCREDIT " + credit.getValue());
        }
        Utils.printThreadStats(Globals.log.getPrintStream());
    }
}
