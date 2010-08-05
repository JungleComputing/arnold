package ibis.arnold;

/**
 * The BitTorrent PIECE message. Contains a chunk of data of a particular piece.
 * (Don't look at me like that, I didn't invent this name.)
 * 
 * @author Kees van Reeuwijk
 * 
 */
class PieceMessage extends Message {
    private static final long serialVersionUID = 1L;

    final CreditValue credit;

    final int piece;

    final int offset;

    final byte data[];

    PieceMessage(final CreditValue credit, final int piece, final int offset,
            final byte data[]) {
        this.credit = credit;
        this.piece = piece;
        this.offset = offset;
        this.data = data;
    }

    @Override
    public String toString() {
        return "PieceMessage[pc=" + piece + ",off=" + offset + ",data.length="
                + data.length + "]";
    }
}
