package ibis.arnold;

import java.io.PrintStream;
import java.util.LinkedList;

class WindowTransferPerformanceMonitor implements TransferPerformanceMonitor {
    private int sampleCount = 0;
    private long sum = 0;
    private final long windowTime;
    private final LinkedList<Sample> samples = new LinkedList<Sample>();

    /**
     * A class to hold a single sample in the performance monitor.
     * 
     * @author Kees van Reeuwijk
     * 
     */
    private static final class Sample {
        private final long value;
        private final long moment;

        private Sample(final long value) {
            this.value = value;
            this.moment = System.currentTimeMillis();
        }
    }

    /**
     * Constructs a new transfer performance monitor with the given time window
     * in milliseconds.
     * 
     * @param windowTime
     *            The time window in milliseconds of the performance monitor.
     */
    WindowTransferPerformanceMonitor(final long windowTime) {
        this.windowTime = windowTime;
    }

    @SuppressWarnings("synthetic-access")
    private void removeObsoleteSamples() {
        final long cutoffTime = System.currentTimeMillis() - windowTime;
        while (!samples.isEmpty()) {
            final Sample s = samples.getFirst();
            if (s.moment > cutoffTime) {
                break; // We should still count this sample.
            }
            sum -= s.value; // Deduct this value from the total value.
            samples.removeFirst();
        }
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public void registerTransfer(final long size) {
        samples.add(new Sample(size));
        sum += size;
        removeObsoleteSamples();
        sampleCount++;
    }

    @Override
    public double estimatePerformance() {
        removeObsoleteSamples();
        return sum / (1e-3 * windowTime);
    }

    @Override
    public void PrintStatistics(final String name, final PrintStream s) {
        s.println(name + ": " + sampleCount + " samples");
    }

}
