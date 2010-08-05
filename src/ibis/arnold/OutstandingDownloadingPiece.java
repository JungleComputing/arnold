/**
 * 
 */
package ibis.arnold;

/**
 * Information about an outstanding download piece.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class OutstandingDownloadingPiece {
    final int piece;
    final PeerInfo peer;

    OutstandingDownloadingPiece(final PeerInfo p, final int piece) {
        this.peer = p;
        this.piece = piece;
    }

    @Override
    public String toString() {
        return Utils.toStringClassScalars(this);
    }
}