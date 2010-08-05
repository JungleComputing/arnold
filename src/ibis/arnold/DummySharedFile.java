package ibis.arnold;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * A dummy implementation of a shared file. This implementation does not have a
 * real file to read or write from, but upon read calculates the values of the
 * bytes from a fixed formula. Upon write it checks whether the stored bytes are
 * equal to the fixed values, and generates an internal error if the are not.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class DummySharedFile implements SharedFileInterface {
    private final long size;
    private final int numberOfPieces;
    private final PieceSet validPieces;
    private final byte referenceHashes[][];
    private final int pieceSize;
    private final int innerBlockSize;
    private final static boolean useBuffer = true;

    DummySharedFile(final int pieceSize, final long desiredSize,
            final byte refHashes[][], final int innerBlockSize)
            throws NoSuchAlgorithmException {
        this.size = desiredSize;
        this.pieceSize = pieceSize;
        this.innerBlockSize = innerBlockSize;
        this.numberOfPieces = Utils.divideRoundUp(desiredSize, pieceSize);
        this.referenceHashes = refHashes;
        if (refHashes != null) {
            checkReferenceHashes(refHashes, desiredSize, pieceSize,
                    innerBlockSize);
        }
        validPieces = new PieceSet(numberOfPieces);
        Globals.log.reportProgress("Created dummy shared file");
    }

    private static void checkReferenceHashes(final byte[][] refHashes,
            final long desiredSize, final int pieceSize,
            final int innerBlockSize) throws NoSuchAlgorithmException {
        final int numberOfPieces = Utils.divideRoundUp(desiredSize, pieceSize);
        if (refHashes.length != numberOfPieces) {
            Globals.log
                    .reportInternalError("Reference hash is of the wrong length: expected "
                            + numberOfPieces + ", got " + refHashes.length);
        }
        for (int i = 0; i < refHashes.length; i++) {
            final long start = (long) pieceSize * i;
            long end = (long) pieceSize * (i + 1);
            if (end > desiredSize) {
                end = desiredSize;
            }
            final int length = (int) (end - start);
            final byte data[] = generateChunk(i, pieceSize, 0, innerBlockSize,
                    length);
            final byte[] digest = Utils.computeSHA1(data, length);
            final boolean valid = Arrays.equals(digest, refHashes[i]);
            if (!valid) {
                Globals.log
                        .reportInternalError("Incorrect reference hash for piece "
                                + i);
            }
        }
    }

    private static long calculateByteno(final int piece, final int pieceSize,
            final int offset) {
        return (long) piece * pieceSize + offset;
    }

    private static byte calculateByte(final int innerBlockSize,
            final long byteno) {
        return (byte) (byteno / innerBlockSize & 0xff);
    }

    private static byte[] generateChunk(final int piece, final int pieceSize,
            final int offset, final int innerBlockSize, final int length) {
        final byte res[] = new byte[length];

        if (useBuffer) {
            Arrays.fill(res, (byte) piece);
        } else {
            final long byteno = calculateByteno(piece, pieceSize, offset);
            for (int i = 0; i < length; i++) {
                res[i] = calculateByte(innerBlockSize, byteno + i);
            }
        }
        return res;
    }

    /**
     * Given a piece number and an offset in that piece, writes the given data
     * to its proper place in the file. We assume that the chunk will never
     * extend beyond the specified size of the file.
     * 
     * @param piece
     *            The piece this data belongs to.
     * @param offset
     *            The offset in the piece of this data.
     * @param data
     *            The data to write.
     */
    private void writeChunk(final int piece, final int offset,
            final byte data[]) {
        final byte refData[] = generateChunk(piece, pieceSize, offset,
                innerBlockSize, data.length);
        if (!Arrays.equals(data, refData)) {
            Globals.log
                    .reportInternalError("Reference data differs from actual data");
        }
    }

    @Override
    public boolean storePiece(final int piece, final byte[] completedPiece)
            throws IOException {
        if (referenceHashes != null) {
            byte hash[];
            try {
                hash = Utils.computeSHA1(completedPiece);
            } catch (final NoSuchAlgorithmException e) {
                Globals.log.reportInternalError("Unknown digest algorithm: "
                        + e.getLocalizedMessage());
                throw new DownloadFailedError("Unknown digest algorithm", e);
            }
            if (!Arrays.equals(hash, referenceHashes[piece])) {
                return false;
            }
        }
        writeChunk(piece, 0, completedPiece);
        validPieces.set(piece);
        return true;
    }

    /**
     * Given a piece number and an offset in that piece, reads the given data
     * from the file. You usually get all the bytes you ask for, but if the
     * official size of the file is larger than specified, you will get a chunk
     * that is truncated to the file size.
     * 
     * @param piece
     *            The piece this data belongs to.
     * @param offset
     *            The offset in the piece of this data.
     * @param length
     *            The length of the data to read.
     */
    private byte[] readChunk(final int piece, final int offset, final int length) {
        return generateChunk(piece, pieceSize, offset, innerBlockSize, length);
    }

    @Override
    public byte[] readChunk(final Chunk chunk) throws IOException {
        return readChunk(chunk.piece, chunk.offset, chunk.size);
    }

    @Override
    public void close() throws IOException {
        // Nothing to do.
    }

    @Override
    public boolean isValidPiece(final int piece) {
        return validPieces.get(piece);
    }

    @Override
    public PieceSet getKnownPieces() {
        return validPieces.clone();
    }

    /**
     * Returns the size of the given piece. For all but the last piece this is
     * the constant PIECE_SIZE. The size of the last one is determined by the
     * length of the shared file.
     * 
     * @param piece
     *            The piece to compute the size for.
     * @return The piece size.
     */
    @Override
    public int getPieceSize(final int piece) {
        if (piece < numberOfPieces - 1) {
            return pieceSize;
        }
        return (int) (size - (long) pieceSize * (numberOfPieces - 1));
    }

    @Override
    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    @Override
    public boolean isComplete() {
        return validPieces.isComplete();
    }

    @Override
    public boolean canSetValid() {
        return true;
    }

    @Override
    public void setValid() {
        validPieces.setComplete();
    }
}
