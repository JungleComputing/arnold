package ibis.arnold;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * A file ready for up- and downloading.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class SharedFile implements SharedFileInterface {
    private final RandomAccessFile rafile;
    private final long size;
    private final int numberOfPieces;
    private final PieceSet validPieces;
    private final byte referenceHashes[][];
    private final int pieceSize;

    SharedFile(final File file, final int pieceSize, final long desiredSize,
            final byte refHashes[][]) throws IOException,
            NoSuchAlgorithmException {
        this.rafile = new RandomAccessFile(file, "rw");
        this.size = desiredSize;
        this.pieceSize = pieceSize;
        this.numberOfPieces = Utils.divideRoundUp(desiredSize, pieceSize);
        this.referenceHashes = refHashes;
        // The file existed when we entered, go and check it.
        this.validPieces = checkPieces(rafile, refHashes, desiredSize,
                pieceSize, numberOfPieces);
        this.rafile.setLength(desiredSize);
    }

    private PieceSet checkPieces(final RandomAccessFile f,
            final byte[][] references, final long desiredSize,
            final int referencePieceSize, final int referenceNumberOfPieces)
            throws IOException, NoSuchAlgorithmException {
        final byte buf[] = new byte[referencePieceSize];

        final PieceSet ourValidPieces = new PieceSet(referenceNumberOfPieces);
        int off = 0;
        int validCount = 0;
        int invalidCount = 0;
        try {
            for (int piece = 0; piece < references.length; piece++) {
                int end = off + referencePieceSize;
                if (end > desiredSize) {
                    end = (int) desiredSize;
                }
                final int len = end - off;
                f.readFully(buf, 0, len);
                off = end;
                final byte[] digest = Utils.computeSHA1(buf, len);
                final boolean valid = Arrays.equals(digest, references[piece]);
                ourValidPieces.set(piece, valid);
                if (valid) {
                    validCount++;
                } else {
                    invalidCount++;
                }
            }
        } catch (final EOFException x) {
            invalidCount += Utils.divideRoundUp(desiredSize - off,
                    referencePieceSize);
        }
        System.out.println("I have " + validCount + " valid and "
                + invalidCount + " invalid pieces");
        return ourValidPieces;
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
     * @throws IOException
     *             Thrown if for some reason the data cannot be written.
     */
    void writeChunk(final int piece, final int offset, final byte data[])
            throws IOException {
        final long pos = pieceSize * piece + offset;
        rafile.seek(pos);
        rafile.write(data);
    }

    @Override
    public boolean storePiece(final int piece, final byte[] completedPiece)
            throws IOException {
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
     * @throws IOException
     *             Thrown if for some reason the data cannot be written.
     */
    byte[] readChunk(final int piece, final int offset, int length)
            throws IOException {
        final long pos = pieceSize * piece + offset;
        if (pos + length > size) {
            // Enforce the required file size: don't return non-existent bytes.
            length = (int) (size - pos);
        }
        rafile.seek(pos);
        final byte buf[] = new byte[length];
        rafile.readFully(buf);
        return buf;
    }

    @Override
    public byte[] readChunk(final Chunk chunk) throws IOException {
        return readChunk(chunk.piece, chunk.offset, chunk.size);
    }

    @Override
    public void close() throws IOException {
        rafile.close();
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
        return false;
    }

    @Override
    public void setValid() {
        Globals.log.reportInternalError("Cannot declare a real file valid");
    }
}
