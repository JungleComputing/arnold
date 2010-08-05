package ibis.arnold;

import java.io.PrintStream;

/**
 * The interface of a transfer performance estimator. Every transfer should be
 * registered to the estimator, and the estimator returns an estimate of
 * resulting performance.
 * 
 * @author Kees van Reeuwijk
 * 
 */
interface TransferPerformanceMonitor {
    /**
     * Registers a transfer of a data block of the given size.
     * 
     * @param size
     *            The size of the data block.
     */
    void registerTransfer(long size);

    /**
     * Returns the current estimated performance in units per second. In case
     * not enough is known, the value <code>Double.NaN</code> is returned. Note
     * that you'll have to use Double.isNaN() to test for this value.
     * 
     * @return The estimated performance, or <code>Double.NaN</code>.
     */
    double estimatePerformance();

    /**
     * Prints some statistics of this estimator to the given stream.
     * 
     * @param name
     *            The name to attach to this performance monitor.
     * 
     * @param s
     *            The stream to print to.
     */
    void PrintStatistics(String name, PrintStream s);
}
