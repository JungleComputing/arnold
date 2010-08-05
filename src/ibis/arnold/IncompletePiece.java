package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * A piece that is under construction.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class IncompletePiece {
    final IbisIdentifier peer;
    final int piece;
    private final int pieceSize;
    private final List<Chunk> queuedChunks = new LinkedList<Chunk>();
    private final List<Chunk> outstandingChunks = new LinkedList<Chunk>();

    /**
     * The data of the piece.
     */
    private final byte data[];

    /**
     * A 'valid' bit for all bytes in <code>data</code>.
     */
    private final BitSet bits = new BitSet();

    void addBytes(final int offset, final byte[] msgdata) {
        System.arraycopy(msgdata, 0, data, offset, msgdata.length);
        bits.set(offset, (offset + msgdata.length));
        final Chunk c = new Chunk(piece, offset, msgdata.length);
        final boolean removed = outstandingChunks.remove(c);
        if (!removed) {
            Globals.log.reportInternalError("Received unrequested " + c
                    + "; outstanding are: "
                    + Arrays.deepToString(outstandingChunks.toArray())
                    + "; queued are: "
                    + Arrays.deepToString(queuedChunks.toArray()));
        }
    }

    boolean isComplete() {
        // The piece is complete if all its bytes are valid.
        return bits.cardinality() >= pieceSize;
    }

    byte[] getPieceBytes() {
        return data;
    }

    IncompletePiece(final IbisIdentifier peer, final int piece,
            final int pieceSize) {
        this.peer = peer;
        this.piece = piece;
        this.pieceSize = pieceSize;
        data = new byte[pieceSize];
        int offset = 0;
        while (offset < pieceSize) {
            int end = offset + Settings.CHUNK_SIZE;
            if (end > pieceSize) {
                end = pieceSize;
            }
            final int size = end - offset;
            final Chunk c = new Chunk(piece, offset, size);
            queuedChunks.add(c);
            offset = end;
        }
    }

    Chunk getNextRequest() {
        if (queuedChunks.isEmpty()) {
            return null;
        }
        final Chunk c = queuedChunks.remove(0);
        outstandingChunks.add(c);
        return c;
    }

    boolean maintainRequests(final Credit credit, final Transmitter transmitter) {
        boolean progress = false;

        while (outstandingChunks.size() < Settings.CHUNK_REQUESTS_PER_PEER) {
            final Chunk c = getNextRequest();
            if (c == null) {
                break;
            }
            final CreditValue saldo = credit.getValue();
            final RequestMessage m = new RequestMessage(saldo, c);
            transmitter.addToRequestQueue(peer, m);
            progress = true;
        }
        return progress;
    }

    boolean hasChunkRequestsReady() {
        return outstandingChunks.size() < Settings.CHUNK_REQUESTS_PER_PEER
                && !queuedChunks.isEmpty();
    }

    void dumpState() {
        Globals.log.reportProgress("Incomplete piece " + piece + " pieceSize="
                + pieceSize + " peer=" + peer + ", " + " outstanding chunks "
                + Arrays.deepToString(outstandingChunks.toArray()));
        final String cl = Arrays.deepToString(queuedChunks.toArray());
        Globals.log.reportProgress(" + queued chunks: " + cl);
    }

    void sendCancels(final Transmitter transmitter) {
        for (final Chunk chunk : outstandingChunks) {
            final CancelMessage msg = new CancelMessage(chunk);
            transmitter.addToRequestQueue(peer, msg);
        }
    }

}
