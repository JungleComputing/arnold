package ibis.arnold;

import ibis.ipl.IbisCreationFailedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Main class of an arnold peer.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class Arnold {
    private static JorrentInfo loadJorrentFile(final File f) {
        try {
            final InputStream fis = new FileInputStream(f);
            final ObjectInputStream in = new ObjectInputStream(fis);
            final JorrentInfo res = (JorrentInfo) in.readObject();
            in.close();
            return res;
        } catch (final IOException e) {
            Globals.log.reportError("Cannot load jorrent file " + f + ": "
                    + e.getLocalizedMessage());
            return null;
        } catch (final ClassNotFoundException e) {
            Globals.log.reportError("Cannot load jorrent file " + f + ": "
                    + e.getLocalizedMessage());
            return null;
        }
    }

    private static void usage(final String msg, final String args[]) {
        System.err.println("Error: " + msg);
        System.err
                .println("Usage: Arnold <option>] ... <option> <jorrent-file> [<file-to-share>]");
        System.err.println("Where <option> is:");
        System.err
                .println(" --dummyfile\tShare a dummy file instead of a real one");
        System.err
                .println(" --proxymode\tRun proxy mode with the leecher hiding behind helpers");
        System.err.println(" --helper\tPeer is a helper in a proxy-mode setup");
        System.err.println("Actual arguments: " + Arrays.deepToString(args));
        throw new Error("Bad command-line arguments");
    }

    private static void setPropertyOption(final String arg) {
        final int eq = arg.indexOf('=');
        final Properties p = System.getProperties();
        if (eq < 0) {
            p.setProperty(arg.substring(2), "");
        } else {
            p.setProperty(arg.substring(2, eq), arg.substring(eq + 1));
        }
    }

    /**
     * @param args
     *            The command-line arguments.
     */
    public static void main(final String[] args) {
        File sharedFileName = null;
        File jorrentFileName = null;
        boolean dummyFile = false;
        boolean altruisticLeechers = false;
        boolean impatientLeechers = false;
        boolean helper = false;
        boolean proxymode = false;
        boolean altruisticHelpers = false;

        // First parse the command line.
        for (final String arg : args) {
            if (arg.startsWith("-D")) {
                setPropertyOption(arg);
            } else if (arg.equalsIgnoreCase("--dummyfile")) {
                dummyFile = true;
            } else if (arg.equalsIgnoreCase("--proxymode")) {
                proxymode = true;
            } else if (arg.equalsIgnoreCase("--helper")) {
                helper = true;
            } else if (arg.equalsIgnoreCase("--helpersStay")) {
                helper = true;
                altruisticHelpers = true;
            } else if (arg.equalsIgnoreCase("--leechersStay")) {
                altruisticLeechers = true;
            } else if (arg.equalsIgnoreCase("--impatientLeechers")) {
                impatientLeechers = true;
            } else if (arg.startsWith("--")) {
                usage("Unknown option '" + arg + '\'', args);
            } else {
                if (jorrentFileName == null) {
                    // We don't have a jorrent file yet.
                    jorrentFileName = new File(arg);
                } else if (sharedFileName == null) {
                    sharedFileName = new File(arg);
                } else {
                    usage("Too many arguments: jorrentFileName='"
                            + jorrentFileName + "' sharedFileName='"
                            + sharedFileName + '\'', args);
                }
            }
        }
        if (helper && !proxymode) {
            usage("A peer can only be a helper in proxy mode", args);
        }
        final SharedFileInterface sharedFile;
        try {
            if (dummyFile) {
                sharedFile = new DummySharedFile(Settings.PIECE_SIZE,
                        Settings.DUMMY_FILE_SIZE, null,
                        Settings.DUMMY_INNER_BLOCKSIZE);
            } else {
                if (jorrentFileName == null) {
                    usage("No jorrent file specified", args);
                }

                if (sharedFileName == null) {
                    try {
                        sharedFileName = File.createTempFile("arnold-",
                                "-dummy-shared-file");
                        sharedFileName.deleteOnExit();
                    } catch (final IOException e) {
                        System.err.println("Cannot create temp file, Goodbye!");
                        e.printStackTrace();
                    }
                } else {
                    if (sharedFileName.exists() && !sharedFileName.isFile()) {
                        System.err.println("Shared file '" + sharedFileName
                                + "' is not a plain file, Goodbye!");
                        System.exit(1);
                    }
                }
                final JorrentInfo info = loadJorrentFile(jorrentFileName);
                if (info == null) {
                    System.err.println("Failed to load jorrent file, Goodbye!");
                    System.exit(1);
                    return;
                }
                System.out.println("Loaded jorrent file " + jorrentFileName);
                sharedFile = new SharedFile(sharedFileName, info.pieceSize,
                        info.length, info.digests);
            }
        } catch (final IOException e) {
            System.err.println("Failed to access shared file");
            e.printStackTrace();
            System.exit(1);
            return;
        } catch (final NoSuchAlgorithmException e) {
            System.err.println("Internal error: unknown digest algorithm: "
                    + e.getLocalizedMessage());
            System.exit(2);
            return;
        }
        try {
            final Engine e = new Engine(sharedFile, proxymode, helper,
                    altruisticHelpers, altruisticLeechers, impatientLeechers);
            e.start();
            e.join();
        } catch (final IOException e) {
            System.err.println("Failed to access shared file");
            e.printStackTrace();
        } catch (final IbisCreationFailedException e) {
            System.err.println("Could not create ibis: "
                    + e.getLocalizedMessage());
            e.printStackTrace();
            System.err.println("Goodbye!");
            System.exit(2);
        } catch (final InterruptedException e) {
            System.err.println("Main thread got interrupt");
            e.printStackTrace();
        }
    }
}
