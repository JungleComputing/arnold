package ibis.arnold;

import java.io.Serializable;

/** A part of a piece to upload or download. */
class Chunk implements Serializable {
    private static final long serialVersionUID = -6623688913279164284L;

    final int piece;
    final int offset;
    final int size;

    Chunk(final int piece, final int offset, final int size) {
        this.piece = piece;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Chunk)) {
            return false;
        }
        final Chunk co = (Chunk) other;
        return this.piece == co.piece && this.offset == co.offset
                && this.size == co.size;
    }

    @Override
    public String toString() {
        return "chunk[pc=" + piece + ",off=0x" + Integer.toHexString(offset)
                + ",sz=0x" + Integer.toHexString(size) + "]";
    }

    @Override
    public int hashCode() {
        return this.piece + this.offset << 1 + this.size << 2;
    }
}
