package ibis.arnold;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A testsuite for Arnold.
 * 
 * @author Kees van Reeuwijk.
 */
public class AllTests {

    /**
     * Constructs a testsuite.
     * 
     * @return The testsuite.
     */
    public static Test suite() {

        final TestSuite suite = new TestSuite("Tests for Arnold");
        // $JUnit-BEGIN$
        suite.addTestSuite(PieceRankerTest.class);
        suite.addTestSuite(UtilsTest.class);
        suite.addTestSuite(InspectionTest.class);
        suite.addTestSuite(OutstandingRequestTest.class);
        suite.addTestSuite(SharedFileTest.class);
        suite.addTestSuite(SharedFileByteNumberingTest.class);
        suite.addTestSuite(IbisSerializationTests.class);
        suite.addTestSuite(WindowTransferPerformanceMonitorTest.class);
        // $JUnit-END$
        return suite;
    }

    static void main(final String args[]) {
        org.junit.runner.JUnitCore.main("AllTests");
    }
}
