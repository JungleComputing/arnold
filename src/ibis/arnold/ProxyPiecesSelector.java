package ibis.arnold;

import ibis.ipl.IbisIdentifier;

/**
 * The interface of piece selectors for proxy downloading. Such a piece selector
 * is run on the anonymization proxy coordinator, and it basically decides which
 * helper should download or upload which pieces.
 * 
 * @author Kees van Reeuwijk
 * 
 */
interface ProxyPiecesSelector {
    /**
     * Adds a new helper to our administration.
     * 
     * @param helper
     *            The helper to add.
     */
    void addHelper(IbisIdentifier helper);

    /**
     * Removes a helper from our administration.
     * 
     * @param helper
     *            The helper to remove.
     */
    void removeHelper(IbisIdentifier helper);

    /**
     * Registers that the given piece was downloaded from the given peer.
     * 
     * @param piece
     *            The piece that we now have.
     */
    void havePiece(int piece);

    /**
     * Returns true iff the selector thinks we need more helpers.
     * 
     * @return Whether there should be more helpers.
     */
    boolean needMoreHelpers();
}
