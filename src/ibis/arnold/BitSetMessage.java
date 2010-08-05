package ibis.arnold;

/**
 * A message containing a list of known pieces.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class BitSetMessage extends Message {
    private static final long serialVersionUID = 1L;

    final PieceSet bits;

    BitSetMessage(final PieceSet bits) {
        this.bits = bits;
    }

    @Override
    public String toString() {
        return "BitsetMessage";
    }
}
