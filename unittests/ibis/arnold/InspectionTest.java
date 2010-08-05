package ibis.arnold;

import ibis.arnold.Utils;
import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests for class inspection using reflection.
 * 
 * @author Kees van Reeuwijk.
 */
public class InspectionTest extends TestCase {
    private static class C1 {
        @SuppressWarnings("unused")
        private final int n;
        @SuppressWarnings("unused")
        public final boolean b;
        @SuppressWarnings("unused")
        private final double d;
        @SuppressWarnings("unused")
        private final char c;

        C1(final int n, final boolean b, final double d, final char c) {
            super();
            this.n = n;
            this.b = b;
            this.d = d;
            this.c = c;
        }
    }

    private static class C2 extends C1 {
        @SuppressWarnings("unused")
        int m;

        C2(final int n, final boolean b, final double d, final char c,
                final int m) {
            super(n, b, d, c);
            this.m = m;
        }

    }

    private static class C3 {
        @SuppressWarnings("unused")
        final String s;

        C3(final String s) {
            this.s = s;
        }
    }

    /**
     * 
     */
    @Test
    public void testInspection() {
        final C1 cl1 = new C1(3, true, -1e-3, 'X');

        final String x1 = Utils.toStringClassScalars(cl1);
        assertEquals("C1[n=3 b=true d=-0.0010 c=X]", x1);

        final C2 cl2 = new C2(5, true, -1e-3, 'Y', -3);

        final String x2 = Utils.toStringClassScalars(cl2);
        assertEquals("C2[n=5 b=true d=-0.0010 c=Y m=-3]", x2);

        final C3 cl3 = new C3("TestXtesT");

        final String x3 = Utils.toStringClassScalars(cl3);
        assertEquals("C3[s=\"TestXtesT\"]", x3);
    }
}
