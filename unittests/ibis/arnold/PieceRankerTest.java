package ibis.arnold;

import ibis.arnold.PieceRanker;
import ibis.arnold.PieceSet;
import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test the shared file by writing a byte pattern to it externally, and then
 * accessing it through the SharedFile class.
 * 
 * @author Kees van Reeuwijk.
 */
public class PieceRankerTest extends TestCase {
    private void assertRank(final PieceRanker r, final String str,
            final Integer... integers) {
        final int elms[] = r.getRanking();
        assertEquals(integers.length, elms.length);
        if (integers.length == elms.length) {
            for (int i = 0; i < elms.length; i++) {
                assertEquals("Element " + i, (int) integers[i], elms[i]);
            }
        }
        assertTrue("PieceRanker is sane", r.rankingIsSane());
        final String s = r.buildRankingString();
        assertEquals(str, s);
    }

    /**
     * 
     */
    @Test
    public void testPieceRanker() {
        final PieceSet s = new PieceSet(4);
        final PieceRanker r = new PieceRanker(s);
        assertRank(r, " 0:[0-3]", 0, 1, 2, 3);
        r.addOccurrenceCount(0);
        assertRank(r, " 0:[1-3] 1:[0]", 1, 2, 3, 0);
        r.removeOccurrenceCount(0);
        assertRank(r, " 0:[0-3]", 0, 1, 2, 3);
        r.addOccurrenceCount(0);
        assertRank(r, " 0:[1-3] 1:[0]", 1, 2, 3, 0);
        r.addOccurrenceCount(1);
        assertRank(r, " 0:[2-3] 1:[0-1]", 2, 3, 0, 1);
        r.addOccurrenceCount(2);
        assertRank(r, " 0:[3] 1:[0-2]", 3, 0, 1, 2);
        r.addOccurrenceCount(3);
        assertRank(r, " 1:[0-3]", 0, 1, 2, 3);
        r.removeOccurrenceCount(2);
        assertRank(r, " 0:[2] 1:[0-1,3]", 2, 0, 1, 3);
        r.removeRank(1);
        assertRank(r, " 0:[2] 1:[0,3]", 2, 0, 3);
    }
}
