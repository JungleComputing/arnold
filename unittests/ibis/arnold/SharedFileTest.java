package ibis.arnold;

import ibis.arnold.Settings;
import ibis.arnold.SharedFile;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests fort he shared file.
 * 
 * @author Kees van Reeuwijk.
 */
public class SharedFileTest extends TestCase {
    private void writeString(final SharedFile sf, final int piece,
            final int offset, final String s) throws IOException {
        final byte buf[] = s.getBytes();
        sf.writeChunk(piece, offset, buf);
    }

    private void assertString(final SharedFile sf, final int piece,
            final int offset, final String ref) throws IOException {
        final int reflen = ref.getBytes().length;
        final byte buf[] = sf.readChunk(piece, offset, reflen);
        final String s = new String(buf);
        assertEquals(ref, s);
    }

    private static byte[][] buildFakeHashes(final int n) {
        return new byte[n][20];
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
        final long size = 2 * Settings.PIECE_SIZE + Settings.PIECE_SIZE / 2;
        final byte hashes[][] = buildFakeHashes(3);
        final SharedFile sf = new SharedFile(f, Settings.PIECE_SIZE, size,
                hashes);
        writeString(sf, 0, 0, "test string");
        writeString(sf, 1, 0, "chunk 1 test string");
        writeString(sf, 2, 0, "chunk 2 test string");
        assertString(sf, 0, 0, "test string");
        assertString(sf, 1, 0, "chunk 1 test string");
        assertString(sf, 2, 0, "chunk 2 test string");
        sf.close();
        assertTrue(f.delete());
    }
}
