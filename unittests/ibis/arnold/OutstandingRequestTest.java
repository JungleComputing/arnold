package ibis.arnold;

import ibis.arnold.Chunk;
import ibis.arnold.IncompletePiece;
import ibis.arnold.Settings;
import ibis.arnold.Utils;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests for the gossip store.
 * 
 * @author Kees van Reeuwijk.
 */
public class OutstandingRequestTest extends TestCase {

    /**
     * Build a reference piece that will be used in subsequent download
     * scenarios.
     * 
     * @return The reference piece.
     */
    private static byte[] buildReferencePiece() {
        final byte data[] = new byte[Settings.PIECE_SIZE];

        byte val = 0;

        for (int i = 0; i < Settings.PIECE_SIZE; i++) {
            data[i] = val++;
        }
        return data;
    }

    private void testDownloadOrder(final byte referenceData[],
            final byte referenceDigest[]) throws NoSuchAlgorithmException {
        final IncompletePiece p = new IncompletePiece(null, 0, referenceData.length);
        while (!p.isComplete()) {
            final Chunk c = p.getNextRequest();
            assertNotNull(c);
            final byte data[] = Arrays.copyOfRange(referenceData, c.offset,
                    c.offset + c.size);
            p.addBytes(c.offset, data);
        }
        assertTrue(p.isComplete());
        final byte pieceData[] = p.getPieceBytes();
        final byte pieceDigest[] = Utils.computeSHA1(pieceData);
        Assert.assertTrue(Arrays.equals(referenceData, pieceData));
        Assert.assertTrue(Arrays.equals(referenceDigest, pieceDigest));
    }

    /**
     * @throws NoSuchAlgorithmException
     *             Thrown if the SHA1 digest algorithm is not known by this Java
     *             implementation.
     * 
     */
    @Test
    public void testOutstandingRequest() throws NoSuchAlgorithmException {
        final byte refData[] = buildReferencePiece();
        final byte refDigest[] = Utils.computeSHA1(refData, refData.length);

        testDownloadOrder(refData, refDigest);
        testDownloadOrder(refData, refDigest);
        testDownloadOrder(refData, refDigest);
        testDownloadOrder(refData, refDigest);
        testDownloadOrder(refData, refDigest);
    }
}
