package ibis.arnold;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * A small program to build a .jorrent file from a given input file. A .jorrent
 * file is similar in function to a .torrent file.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class BuildJorrentFile {
    private static final long MAXIMAL_FILE_SIZE = (long) Settings.PIECE_SIZE
            * Integer.MAX_VALUE;

    private static void writeJorrentFile(final File f, final JorrentInfo content)
            throws FileNotFoundException, IOException {
        final OutputStream fis = new FileOutputStream(f);
        final ObjectOutputStream out = new ObjectOutputStream(fis);
        out.writeObject(content);
        out.close();
    }

    private static JorrentInfo buildJorrentInfo(final File f,
            final int pieceSize) throws IOException, NoSuchAlgorithmException {
        final long sz = f.length();

        if (sz >= MAXIMAL_FILE_SIZE) {
            System.err.println("File too large");
            System.exit(1);
        }
        final int pieces = Utils.divideRoundUp(sz, pieceSize);
        final byte digests[][] = new byte[pieces][];

        final FileInputStream s = new FileInputStream(f);
        try {
            final byte buf[] = new byte[pieceSize];
            int off = 0;

            for (int i = 0; i < pieces; i++) {
                final int readsz = s.read(buf);
                if (i < pieces - 1 && readsz != pieceSize) {
                    System.err.println("Short read");
                }
                off += pieceSize;
                digests[i] = Utils.computeSHA1(buf, readsz);
            }
            return new JorrentInfo(pieceSize, sz, digests);
        } finally {
            s.close();
        }
    }

    /**
     * @param args
     *            The command-line arguments.
     */
    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err
                    .println("Usage: BuildJorrentFile <jorrent-file> <file-to-share>");
            System.err
                    .println("Actual arguments: " + Arrays.deepToString(args));
            System.exit(1);
        }
        final File jorrentFile = new File(args[0]);
        final File sharedFile = new File(args[1]);

        if (!sharedFile.exists()) {
            System.err.println("File '" + sharedFile + "' does not exist");
            System.out.println("Giving up");
            System.exit(1);
        }
        if (!sharedFile.isFile()) {
            System.err.println("'" + sharedFile + "' is not a plain file");
            System.out.println("Giving up");
            System.exit(1);
        }
        JorrentInfo info = null;
        final long startTime = System.currentTimeMillis();
        try {
            System.out.println("Constructing digests over " + sharedFile);
            // FIXME: make piece size variable when making .jorrent file.
            final int pieceSize = Settings.PIECE_SIZE;
            info = buildJorrentInfo(sharedFile, pieceSize);
        } catch (final NoSuchAlgorithmException x) {
            System.err.println("Internal error: unknown digest algorithm: "
                    + x.getLocalizedMessage());
            System.exit(2);
        } catch (final IOException e) {
            System.err.println("Cannot read file '" + sharedFile + "':"
                    + e.getLocalizedMessage());
            System.out.println("Giving up");
            System.exit(1);
        }
        try {
            System.out.println("Writing jorrent file " + jorrentFile);
            writeJorrentFile(jorrentFile, info);
        } catch (final IOException e) {
            System.err.println("Cannot write file '" + jorrentFile + "':"
                    + e.getLocalizedMessage());
            System.out.println("Giving up");
            System.exit(1);
        }
        final long duration = System.currentTimeMillis() - startTime;
        System.out.println("Done. It took "
                + Utils.formatSeconds(duration * 1e-3));
    }

}
