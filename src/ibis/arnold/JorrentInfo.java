package ibis.arnold;

import java.io.Serializable;

/**
 * A class that stores the equivalent of a .torrent file. Designed to be
 * serializable, so that the relevant info can be read and written from/to file
 * in one go.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class JorrentInfo implements Serializable {
    private static final long serialVersionUID = 2L;

    final int pieceSize;

    final long length;

    final byte[][] digests;

    JorrentInfo(final int pieceSize, final long length, final byte[][] digests) {
        this.pieceSize = pieceSize;
        this.length = length;
        this.digests = digests;
    }
}
