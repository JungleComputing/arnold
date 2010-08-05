package ibis.arnold;

import ibis.arnold.Settings;
import ibis.arnold.SharedFile;
import ibis.arnold.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test the shared file by writing a byte pattern to it externally, and then
 * accessing it through the SharedFile class.
 * 
 * @author Kees van Reeuwijk.
 */
public class SharedFileByteNumberingTest extends TestCase {
    private static final int INNER_REPEAT_COUNT = 10;
    private static final int OUTER_REPEAT_COUNT = 100;

    private static byte[] createTestArray() {
        final byte buf[] = new byte[INNER_REPEAT_COUNT * 256
                * OUTER_REPEAT_COUNT];
        int offset = 0;

        for (int i = 0; i < OUTER_REPEAT_COUNT; i++) {
            for (int val = 0; val < 256; val++) {
                final byte b = (byte) val;
                final int end = offset + INNER_REPEAT_COUNT;
                Arrays.fill(buf, offset, end, b);
                offset = end;
            }
        }
        return buf;
    }

    /**
     * Create a file with a specific pattern: 10 bytes 0x00, 10 bytes 0x01, etc.
     * This pattern is 2560 bytes long, and is repeated 100 times for a total
     * file size of 256000 bytes.
     * 
     * @param f
     *            The file to write to.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private static byte[][] createTestFile(final File f) throws IOException,
            NoSuchAlgorithmException {
        final byte buf[] = createTestArray();
        final byte[][] hashes = new byte[1][];
        final FileOutputStream s = new FileOutputStream(f);

        s.write(buf);
        s.close();
        assertEquals(INNER_REPEAT_COUNT * 256 * OUTER_REPEAT_COUNT, f.length());
        hashes[0] = Utils.computeSHA1(buf);
        return hashes;
    }

    private static final byte[] toByteValues(final int val[]) {
        final byte res[] = new byte[val.length];

        for (int i = 0; i < val.length; i++) {
            res[i] = (byte) val[i];
        }
        return res;
    }

    private static final void assertBytes(final SharedFile sf, final int piece,
            final int offset, final int values[]) throws IOException {
        final byte ref[] = toByteValues(values);
        final byte reads[] = sf.readChunk(piece, offset, ref.length);
        assertTrue(Arrays.equals(ref, reads));
    }

    /**
     * @throws IOException
     *             Thrown if File access fails.
     * @throws NoSuchAlgorithmException
     *             Thrown if the SHA-1 digest algorithm is unknown.
     * 
     */
    @Test
    public void testSharedFile() throws NoSuchAlgorithmException, IOException {
        final File f = File.createTempFile("Arnold", "test");
        final byte hashes[][] = createTestFile(f);
        final SharedFile sf = new SharedFile(f, Settings.PIECE_SIZE, f.length(),
                hashes);
        assertTrue("The test file fits in one piece",
                f.length() < Settings.PIECE_SIZE);
        assertTrue("Shared file with correct pieces", sf.isValidPiece(0));
        int vals[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1 };
        assertBytes(sf, 0, 0, vals);
        assertBytes(sf, 0, 256 * INNER_REPEAT_COUNT, vals);
        vals = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2 };
        assertBytes(sf, 0, 10, vals);
        assertBytes(sf, 0, 10 + 256 * INNER_REPEAT_COUNT, vals);
        vals = new int[] { 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 12, 12 };
        assertBytes(sf, 0, 110, vals);
        assertBytes(sf, 0, 110 + 256 * INNER_REPEAT_COUNT, vals);
        sf.close();
        assertTrue(f.delete());
    }
}
