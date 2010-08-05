package ibis.arnold;

import java.io.IOException;

interface SharedFileInterface {

    void close() throws IOException;

    /**
     * Returns true iff the shared file is complete. That is iff all pieces in
     * the file are known.
     * 
     * @return <code>true</code> iff the shared file is complete.
     */
    boolean isComplete();

    /**
     * Returns a bitset where all known pieces are set.
     * 
     * @return The set of known pieces.
     */
    PieceSet getKnownPieces();

    /**
     * Returns the number of pieces in this file.
     * 
     * @return The number of pieces.
     */
    int getNumberOfPieces();

    /**
     * Given a piece number, returns the size of the piece. The returned size is
     * almost always equal to the piece size in the .jorrent file, but the final
     * piece is usually smaller.
     * 
     * @param piece
     *            The piece we want to know the size of.
     * @return The size of the piece.
     */
    int getPieceSize(int piece);

    /**
     * Given a piece number, return true iff this piece is valid.
     * 
     * @param piece
     *            The piece to test.
     * @return <code>true</code> iff the piece is valid.
     */
    boolean isValidPiece(int piece);

    /**
     * Returns data for the given chunk.
     * 
     * @param chunk
     *            The chunk to get data for.
     * @return The data of the chunk.
     * @throws IOException
     *             Thrown if for some reason the data cannot be read.
     */
    byte[] readChunk(Chunk chunk) throws IOException;

    /**
     * Stores the given data for the given piece, and verify it against the
     * reference data.
     * 
     * @param piece
     *            The piece to store the data for.
     * @param data
     *            The data in the piece.
     * @return <code>true</code> if this data is correct according to the
     *         reference data.
     * @throws IOException
     *             Thrown if for some reason the data cannot be stored.
     */
    boolean storePiece(int piece, byte[] data) throws IOException;

    boolean canSetValid();

    void setValid();

}
