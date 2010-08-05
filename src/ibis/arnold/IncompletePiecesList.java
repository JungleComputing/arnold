package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;

class IncompletePiecesList {
    private final ArrayList<IncompletePiece> requests = new ArrayList<IncompletePiece>();

    byte[] updatePiece(final IbisIdentifier source, final int piece,
            final int offset, final byte data[]) {
        boolean sawPiece = false;
        byte pieceData[] = null;
        int ix = requests.size();
        while (ix > 0) {
            ix--;
            final IncompletePiece r = requests.get(ix);
            if (r.piece == piece && r.peer.equals(source)) {
                if (sawPiece) {
                    Globals.log.reportInternalError("Duplicate pieces for pc="
                            + piece + ",off=" + offset);
                }
                r.addBytes(offset, data);
                if (r.isComplete()) {
                    pieceData = r.getPieceBytes();
                    requests.remove(ix);
                }
                sawPiece = true;
            }
        }
        return pieceData;
    }

    /**
     * Removes all outstanding requests to the given peer.
     * 
     * @param peer
     *            The peer for which the requests must be removed.
     * @param scheduler
     *            The scheduler of this node.
     */
    void removePeer(final IbisIdentifier peer,
            final SchedulerInterface scheduler) {
        int ix = requests.size();

        while (ix > 0) {
            ix--;

            final IncompletePiece r = requests.get(ix);
            if (r.isComplete()) {
                Globals.log.reportInternalError("Piece " + r
                        + " is still in the incomplete list, but is complete");
            } else if (r.peer.equals(peer)) {
                scheduler.registerFailedPieceDownload(peer, r.piece);
                requests.remove(ix);
            }
        }
    }

    /**
     * Removes the given piece download.
     * 
     * @param peer
     *            The peer for which the requests must be removed.
     * @param piece
     *            The piece number to of the requests to cancel.
     * @param transmitter
     *            The transmitter to send cancels to.
     */
    void cancelPieceDownload(final IbisIdentifier peer, final int piece,
            final Transmitter transmitter) {
        int ix = requests.size();

        while (ix > 0) {
            ix--;

            final IncompletePiece r = requests.get(ix);
            if (r.piece == piece && r.peer.equals(peer)) {
                requests.remove(ix);
                r.sendCancels(transmitter);
            }
        }
    }

    /**
     * Removes all outstanding requests for the given piece.
     * 
     * @param piece
     *            The piece number to of the requests to cancel.
     * @param transmitter
     *            The transmitter to send cancels to.
     */
    void cancelPieceDownload(final int piece, final Transmitter transmitter) {
        int ix = requests.size();

        while (ix > 0) {
            ix--;

            final IncompletePiece r = requests.get(ix);
            if (r.piece == piece) {
                requests.remove(ix);
                r.sendCancels(transmitter);
            }
        }
    }

    void add(final IbisIdentifier peer, final int piece, final int pieceSize) {
        final IncompletePiece r = new IncompletePiece(peer, piece, pieceSize);
        requests.add(r);
    }

    boolean maintainRequests(final Credit credit, final Transmitter transmitter) {
        boolean progress = false;

        for (final IncompletePiece r : requests) {
            progress |= r.maintainRequests(credit, transmitter);
        }
        return progress;
    }

    boolean requestsToSubmit() {
        for (final IncompletePiece r : requests) {
            if (r.hasChunkRequestsReady()) {
                return true;
            }
        }
        return false;
    }

    void dumpState() {
        if (requests.isEmpty()) {
            Globals.log.reportProgress("(no incomplete pieces)");
            return;
        }
        for (final IncompletePiece p : requests) {
            p.dumpState();
        }
    }

}
