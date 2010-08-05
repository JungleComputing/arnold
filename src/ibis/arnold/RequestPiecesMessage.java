package ibis.arnold;

/**
 * A message containing a list of pieces a helper should download for us.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class RequestPiecesMessage extends Message {
    private static final long serialVersionUID = 1L;

    final PieceSet bits;

    RequestPiecesMessage(final PieceSet bits) {
        this.bits = bits;
    }

    @Override
    public String toString() {
        return "BitsetMessage[" + bits.compactBitSetToString() + "]";
    }
}
