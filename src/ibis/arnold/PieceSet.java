package ibis.arnold;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A set of pieces.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class PieceSet implements Serializable, PieceSetViewer, Iterable<Integer> {
    private static final long serialVersionUID = -4079186808549453173L;
    private final boolean flags[];

    private PieceSet(final PieceSet s) {
        this.flags = Arrays.copyOf(s.flags, s.flags.length);
    }

    PieceSet(final int numberOfPieces) {
        this.flags = new boolean[numberOfPieces];
    }

    private PieceSet(final boolean[] flags) {
        this.flags = flags;
    }

    @Override
    public int cardinality() {
        int res = 0;
        for (final boolean b : flags) {
            if (b) {
                res++;
            }
        }
        return res;
    }

    @Override
    protected PieceSet clone() {
        return new PieceSet(this);
    }

    void and(final PieceSetViewer s) {
        for (int i = 0; i < flags.length; i++) {
            flags[i] &= s.get(i);
        }
    }

    void or(final PieceSetViewer s) {
        for (int i = 0; i < flags.length; i++) {
            flags[i] |= s.get(i);
        }
    }

    @Override
    public boolean isEmpty() {
        for (final boolean b : flags) {
            if (b) {
                return false;
            }
        }
        return true;
    }

    boolean isComplete() {
        for (final boolean b : flags) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    void clear(final int i) {
        flags[i] = false;
    }

    @Override
    public boolean get(final int ix) {
        return flags[ix];
    }

    void set(final int ix) {
        flags[ix] = true;
    }

    void set(final int ix, final boolean val) {
        flags[ix] = val;
    }

    @Override
    public boolean intersects(final PieceSet s) {
        final int len = Math.min(flags.length, s.flags.length);
        for (int i = 0; i < len; i++) {
            if (flags[i] && s.flags[i]) {
                return true;
            }
        }
        return false;
    }

    String compactBitSetToString() {
        final StringBuffer buf = new StringBuffer();
        buf.append('{');
        boolean first = true;
        int i = 0;
        while (i < flags.length) {
            final int start = nextSetBit(i);
            int end = nextClearBit(start + 1);
            if (end < 0) {
                end = flags.length - 1;
            }
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(start);
            if (start + 1 < end) {
                buf.append('-');
                buf.append(end - 1);
            }
            i = end;
        }
        buf.append('}');
        return buf.toString();
    }

    int nextSetBit(int i) {
        while (i < flags.length) {
            if (flags[i]) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private int nextClearBit(int i) {
        while (i < flags.length) {
            if (!flags[i]) {
                return i;
            }
            i++;
        }
        return -1;
    }

    PieceSet getInverse() {
        final boolean l[] = new boolean[flags.length];
        for (int i = 0; i < l.length; i++) {
            l[i] = !flags[i];
        }
        return new PieceSet(l);
    }

    void setComplete() {
        Arrays.fill(flags, true);
    }

    private final static class PieceSetIterator implements Iterator<Integer> {
        private final PieceSet s;
        private int ix;

        private PieceSetIterator(final PieceSet s) {
            this.s = s;
            ix = s.nextSetBit(0);
        }

        @Override
        public boolean hasNext() {
            return ix >= 0;
        }

        @Override
        public Integer next() {
            final int res = ix;
            ix = s.nextSetBit(ix + 1);
            return res;
        }

        @Override
        public void remove() {
            s.clear(ix);
        }

    }

    @SuppressWarnings("synthetic-access")
    @Override
    public Iterator<Integer> iterator() {
        return new PieceSetIterator(this);
    }

    int size() {
        return flags.length;
    }
}
