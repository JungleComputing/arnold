package ibis.arnold;

import ibis.arnold.Utils;

import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests for utility functions.
 * 
 * @author Kees van Reeuwijk.
 */
public class UtilsTest extends TestCase {
    private static void assertBytesToHexString(final byte input[],
            final String result) {
        final String s = Utils.bytesToHexString(input);
        assertEquals(s, result);
    }

    private static void assertSHA1(final String input, final String digest) {
        final byte data[] = input.getBytes();
        try {
            final byte result[] = Utils.computeSHA1(data);
            final String hexResult = Utils.bytesToHexString(result);
            assertEquals(hexResult, digest);
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
            assertTrue("No such algorithm", false);
        }
    }

    /**
     * 
     */
    @Test
    public void testUtils() {
        final byte input0[] = {};
        assertBytesToHexString(input0, "");
        final byte input1[] = { 1 };
        assertBytesToHexString(input1, "01");
        final byte input2[] = { 0, 1, 127 };
        assertBytesToHexString(input2, "00017F");
        final byte input3[] = { 0, 1, 127, -1, -128 };
        assertBytesToHexString(input3, "00017FFF80");
        assertSHA1("", "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709");
        assertSHA1("test", "A94A8FE5CCB19BA61C4C0873D391E987982FBBD3");
        assertSHA1(
                "sdfsdfdsfdsf sdf sfdsfds fsdfds fdsf sdfdsfdsfdsfds fdsfdsfdsfdsfdsfds",
                "1EEE3E13BBB9726A49A792FBFA74C944811B9D2A");
    }
}
