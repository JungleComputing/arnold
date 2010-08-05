package ibis.arnold;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

class WatchdogTimer extends Thread {
    private boolean didReset = false;
    private boolean stopped = false;
    private final long interval;

    WatchdogTimer(final long interval) {
        super("Watchdog timer thread");
        setDaemon(true);
        this.interval = interval;
        setPriority(Thread.MAX_PRIORITY);
    }

    synchronized void reset() {
        didReset = true;
        notifyAll();
    }

    synchronized void setStopped() {
        stopped = true;
        notifyAll();
    }

    private void dumpThreadStatus(final ThreadInfo th) {
        System.out.println("Thread " + th.getThreadId() + " ("
                + th.getThreadName() + ')');
        System.out.println("  state: " + th.getThreadState());
        final long lockOwnerId = th.getLockOwnerId();
        if (lockOwnerId >= 0) {
            System.out.println("  locked by: " + lockOwnerId + " ("
                    + th.getLockOwnerName() + ")");
        }
        System.out.println("  blockedCount=" + th.getBlockedCount()
                + " blockedTime="
                + Utils.formatSeconds(1e-3 * th.getBlockedTime()));
        System.out.println("  waitedCount=" + th.getWaitedCount()
                + " waitedTime="
                + Utils.formatSeconds(1e-3 * th.getWaitedTime()));
        final StackTraceElement[] stackTrace = th.getStackTrace();
        System.out.println("  stack trace:");
        for (final StackTraceElement e : stackTrace) {
            System.out.println("    " + e);
        }
        System.out.println();
    }

    private void dumpStatus() {
        final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threads = mbean.dumpAllThreads(false, false);
        for (final ThreadInfo thread : threads) {
            dumpThreadStatus(thread);
        }

    }

    private synchronized boolean isStopped() {
        return stopped;
    }

    @Override
    public void run() {
        while (!isStopped()) {
            try {
                boolean dumpStatus = false;
                synchronized (this) {
                    didReset = false;
                    wait(interval);
                    dumpStatus = !didReset && !stopped;
                }
                if (dumpStatus) {
                    dumpStatus();
                }
            } catch (final InterruptedException e) {
                // ignore.
            }
        }

    }

}
