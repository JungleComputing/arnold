package ibis.arnold;

/**
 * This message is sent to announce that a peer now has the given piece.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class HaveMessage extends SmallMessage {
    private static final long serialVersionUID = 1L;

    final int piece;

    HaveMessage(final int piece) {
        this.piece = piece;
    }
}
