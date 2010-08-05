package ibis.arnold;

import ibis.arnold.WindowTransferPerformanceMonitor;
import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests for the window-based transfer performance monitor.
 * 
 * @author Kees van Reeuwijk.
 */
public class WindowTransferPerformanceMonitorTest extends TestCase {

    /**
     * 
     */
    @Test
    public void testMonitor() {
        /**
         * Chose a window time that allows us plenty of time for the test.
         */
        final long windowTime = 100; // 100 seconds
        final double delta = 0.5;
        final WindowTransferPerformanceMonitor m = new WindowTransferPerformanceMonitor(
                windowTime * 1000);
        m.registerTransfer(1000L);
        double v = m.estimatePerformance();
        assertEquals(1000.0 / windowTime, v, delta);
        m.registerTransfer(1000L);
        v = m.estimatePerformance();
        assertEquals(2000.0 / windowTime, v, delta);
        m.registerTransfer(500L);
        v = m.estimatePerformance();
        assertEquals(2500.0 / windowTime, v, delta);
        m.registerTransfer(2500L);
        v = m.estimatePerformance();
        assertEquals(5000.0 / windowTime, v, delta);
    }
}
