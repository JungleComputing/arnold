package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;

class RoundRobinProxyPiecesSelector implements ProxyPiecesSelector {
    private final Transmitter transmitter;
    private final int numberOfPieces;

    /**
     * Per piece, the number of responsible helpers, or -1 if the piece is known
     * to us.
     */
    private final int responsibleHelpers[];

    private final ArrayList<HelperInfo> helpers = new ArrayList<HelperInfo>();

    private static final class HelperInfo {
        private final IbisIdentifier helper;
        private final PieceSet pieces;

        private HelperInfo(final IbisIdentifier helper, final PieceSet pieceSet) {
            this.helper = helper;
            this.pieces = pieceSet;
        }
    }

    private static int[] buildResponsibleHelpers(final int numberOfPieces,
            final PieceSet knownPieces) {
        final int res[] = new int[numberOfPieces];
        for (int i = 0; i < numberOfPieces; i++) {
            res[i] = knownPieces.get(i) ? -1 : 0;
        }
        return res;
    }

    RoundRobinProxyPiecesSelector(final Transmitter transmitter,
            final int numberOfPieces, final PieceSet knownPieces) {
        this.transmitter = transmitter;
        this.numberOfPieces = numberOfPieces;
        this.responsibleHelpers = buildResponsibleHelpers(numberOfPieces,
                knownPieces);
    }

    /**
     * Returns the lowest number of responsible helpers mentioned in this list,
     * or -1 if all entries are -1.
     * 
     * @param l
     *            The helpers count list.
     * @param res2
     * @return The lowest number of responsible helpers.
     */
    private static int getLowestResponsibleHelpers(final int l[],
            final PieceSet res2) {
        int res = Integer.MAX_VALUE;

        for (int i = 0; i < l.length; i++) {
            final int n = l[i];
            if (!res2.get(i) && n >= 0 && res > n) {
                res = n;
            }
        }
        if (res == Integer.MAX_VALUE) {
            // All entries are -1
            res = -1;
        }
        return res;
    }

    /**
     * Given the number of pieces that this helper should contribute, return a
     * bitset with the appropriate number of pieces.
     * 
     * @return The set of pieces.
     */
    private PieceSet constructHelperSet() {
        final PieceSet res = new PieceSet(numberOfPieces);
        boolean progress;

        do {
            progress = false;
            final int low = getLowestResponsibleHelpers(responsibleHelpers, res);
            if (low < 0) {
                break;
            }
            for (int i = 0; i < numberOfPieces; i++) {
                if (responsibleHelpers[i] == low && !res.get(i)) {
                    // Add this piece to our set.
                    res.set(i);
                    progress = true;
                    responsibleHelpers[i]++;
                }
            }
        } while (progress);
        return res;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public void addHelper(final IbisIdentifier helper) {
        final PieceSet pieceSet = constructHelperSet();
        final HelperInfo i = new HelperInfo(helper, pieceSet);
        helpers.add(i);
        final RequestPiecesMessage msg = new RequestPiecesMessage(pieceSet);
        transmitter.addToBookkeepingQueue(helper, msg);
    }

    @Override
    public void havePiece(final int piece) {
        responsibleHelpers[piece] = -1;
    }

    /**
     * Returns <code>true</code> iff we need more helpers for this download.
     */
    @Override
    public boolean needMoreHelpers() {
        // Return true iff there is a piece that has zero responsible helpers.
        for (int i = 0; i < numberOfPieces; i++) {
            if (responsibleHelpers[i] == 0) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("synthetic-access")
    private static int findHelper(final ArrayList<HelperInfo> l,
            final IbisIdentifier helper) {
        for (int ix = 0; ix < l.size(); ix++) {
            final HelperInfo i = l.get(ix);
            if (i.helper.equals(helper)) {
                return ix;
            }
        }
        return -1;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public void removeHelper(final IbisIdentifier helper) {
        final int ix = findHelper(helpers, helper);
        if (ix < 0) {
            Globals.log.reportInternalError("Unknown helper " + helper
                    + " removed");
            return;
        }
        final HelperInfo i = helpers.remove(ix);

        final PieceSet pieces = i.pieces;
        for (int p = pieces.nextSetBit(0); p >= 0; p = pieces.nextSetBit(p + 1)) {
            // operate on index i here
            responsibleHelpers[p]--;
        }
    }
}
