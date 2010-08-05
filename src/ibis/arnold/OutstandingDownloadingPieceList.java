/**
 * 
 */
package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * The list of outstanding pieces.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class OutstandingDownloadingPieceList implements
        Iterable<OutstandingDownloadingPiece> {
    private final ArrayList<OutstandingDownloadingPiece> l = new ArrayList<OutstandingDownloadingPiece>();

    void add(final OutstandingDownloadingPiece p) {
        l.add(p);
    }

    int size() {
        return l.size();
    }

    @Override
    public String toString() {
        return Arrays.deepToString(l.toArray());
    }

    void removePeer(final PeerInfo peer, final PieceSet missingPieces,
            final PieceRanker pieceRanker) {
        int ix = l.size();
        while (ix > 0) {
            ix--;
            final OutstandingDownloadingPiece p = l.get(ix);
            if (p.peer == peer) {
                l.remove(ix);
                pieceRanker.registerDownloadCancel(p.piece);
                if (!containsPiece(p.piece)) {
                    // There are no other downloads for this piece.
                    // Set it as missing again.
                    missingPieces.set(p.piece);
                }
            }
        }
    }

    OutstandingDownloadingPiece extractPiece(final IbisIdentifier peer,
            final int piece) {
        int ix = l.size();
        OutstandingDownloadingPiece res = null;
        while (ix > 0) {
            ix--;
            final OutstandingDownloadingPiece p = l.get(ix);
            if (p.piece == piece && peer.equals(p.peer.peer)) {
                l.remove(ix);
                if (res != null) {
                    Globals.log
                            .reportInternalError("Duplicate outstanding piece "
                                    + p);
                }
                res = p;
            }
        }
        return res;
    }

    OutstandingDownloadingPiece get(final int ix) {
        return l.get(ix);
    }

    @Override
    public Iterator<OutstandingDownloadingPiece> iterator() {
        return l.iterator();
    }

    OutstandingDownloadingPiece remove(final int ix) {
        return l.remove(ix);
    }

    boolean containsPiece(final int piece) {
        for (final OutstandingDownloadingPiece p : l) {
            if (p.piece == piece) {
                return true;
            }
        }
        return false;
    }

    int cancelPiece(final int piece, final PeerInfoList neighbors) {
        int n = 0;
        int ix = l.size();
        while (ix > 0) {
            ix--;
            final OutstandingDownloadingPiece p = l.get(ix);
            if (p.piece == piece) {
                neighbors.cancelPiece(p.peer.peer, piece);
                l.remove(ix);
                n++;
            }
        }
        return n;
    }

    /**
     * Given a peer, returns true iff this list of outstanding pieces contains
     * one for the given peer.
     * 
     * @param peer
     *            The peer we're checking for.
     * @return <code>true</code> iff the list contains pieces for this peer.
     */
    boolean containsPiecesFromPeer(final IbisIdentifier peer) {
        for (final OutstandingDownloadingPiece p : l) {
            if (peer.equals(p.peer.peer)) {
                return true;
            }
        }
        return false;
    }

    int countReplication(final int piece) {
        int n = 0;
        for (final OutstandingDownloadingPiece p : l) {
            if (p.piece == piece) {
                n++;
            }
        }
        return n;
    }

    boolean containsPieceFromPeer(final int piece, final IbisIdentifier peer) {
        for (final OutstandingDownloadingPiece p : l) {
            if (p.piece == piece && peer.equals(p.peer.peer)) {
                return true;
            }
        }
        return false;
    }

    PieceSet getOutstandingPieces(final int numberOfPieces) {
        final PieceSet pieces = new PieceSet(numberOfPieces);
        for (final OutstandingDownloadingPiece p : l) {
            pieces.set(p.piece);
        }
        return pieces;
    }
}