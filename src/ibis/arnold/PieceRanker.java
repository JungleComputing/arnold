package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.util.Arrays;

/**
 * Rank pieces based on their rarity. Pieces we have ourselves are considered
 * not rare at all, and are removed from the administration.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class PieceRanker {
    private final int rank[]; // Piece number to index in 'pieces'
    private final PieceInfo pieces[];
    private int validPieces;

    private static final class PieceInfo implements Comparable<PieceInfo> {
        private int occurrences = 0;
        private int downloads = 0;
        private final int pieceNo;

        private PieceInfo(final int pieceNo) {
            this.pieceNo = pieceNo;
        }

        private void addOccurrence() {
            occurrences++;
        }

        @Override
        public int compareTo(final PieceInfo o) {
            if (this.occurrences < o.occurrences) {
                return -1;
            }
            if (this.occurrences > o.occurrences) {
                return 1;
            }
            if (this.pieceNo < o.pieceNo) {
                return -1;
            }
            if (this.pieceNo > o.pieceNo) {
                return 1;
            }
            return 0;
        }

        private void removeOccurrence() {
            occurrences--;
        }

        private void registerDownloadStart() {
            downloads++;
        }

        private void registerDownloadCancel() {
            downloads--;
        }

        @Override
        public String toString() {
            return Utils.toStringClassScalars(this);
        }

    }

    @SuppressWarnings("synthetic-access")
    PieceRanker(final PieceSet pieceSet) {
        final int numberOfPieces = pieceSet.size();
        rank = new int[numberOfPieces];
        pieces = new PieceInfo[numberOfPieces];
        Arrays.fill(rank, -1);
        int ix = 0;
        for (int i = 0; i < numberOfPieces; i++) {
            if (!pieceSet.get(i)) {
                pieces[ix] = new PieceInfo(i);
                rank[i] = ix;
                ix++;
            }
        }
        validPieces = ix;
    }

    /**
     * Build the index array again, it was somehow corrupted.
     */
    @SuppressWarnings("synthetic-access")
    private void buildIndex() {
        Arrays.fill(rank, -1);
        for (int i = 0; i < validPieces; i++) {
            rank[pieces[i].pieceNo] = i;
        }
    }

    /**
     * Adds one occurrence count for the given piece.
     * 
     * @param piece
     *            The piece.
     */
    @SuppressWarnings("synthetic-access")
    void addOccurrenceCount(final int piece) {
        int ix = rank[piece];
        if (ix >= 0) {
            if (pieces[ix] == null || pieces[ix].pieceNo != piece) {
                Globals.log.reportInternalError("Index corrupt; rebuilding");
                buildIndex();
                ix = rank[piece];
            }
            pieces[ix].addOccurrence();
            while (ix + 1 < validPieces) {
                if (pieces[ix].compareTo(pieces[ix + 1]) < 0) {
                    // Piece is in place.
                    break;
                }
                final PieceInfo tmp = pieces[ix + 1];
                pieces[ix + 1] = pieces[ix];
                pieces[ix] = tmp;
                rank[tmp.pieceNo]--;
                rank[piece]++;
                ix++;

            }
        }
    }

    void addOccurrenceCount(final PieceSet set) {
        for (final int elm : set) {
            addOccurrenceCount(elm);
        }
    }

    /**
     * Removes one occurrence count for the given piece.
     * 
     * @param piece
     *            The piece.
     */
    @SuppressWarnings("synthetic-access")
    void removeOccurrenceCount(final int piece) {
        int ix = rank[piece];
        if (ix >= 0) {
            if (pieces[ix] == null || pieces[ix].pieceNo != piece) {
                Globals.log.reportInternalError("Index corrupt; rebuilding");
                buildIndex();
                ix = rank[piece];
            }
            pieces[ix].removeOccurrence();
            while (ix > 0 && pieces[ix].compareTo(pieces[ix - 1]) < 0) {
                final PieceInfo tmp = pieces[ix - 1];
                pieces[ix - 1] = pieces[ix];
                pieces[ix] = tmp;
                rank[tmp.pieceNo]++;
                rank[piece]--;
                ix--;
            }
        }
    }

    void removeOccurrenceCount(final PieceSet set) {
        for (final int elm : set) {
            removeOccurrenceCount(elm);
        }
    }

    /** Completely remove the given piece from our ranking. */
    @SuppressWarnings("synthetic-access")
    void removeRank(final int piece) {
        final int ix = rank[piece];
        if (ix >= 0) {
            rank[piece] = -1;
            System.arraycopy(pieces, ix + 1, pieces, ix, validPieces - ix - 1);
            validPieces--;
            for (int i = ix; i < validPieces; i++) {
                final PieceInfo p = pieces[i];
                rank[p.pieceNo] = i;
            }
            pieces[validPieces] = null;
        }
    }

    @SuppressWarnings("synthetic-access")
    int[] getRanking() {
        final int res[] = new int[validPieces];
        for (int i = 0; i < validPieces; i++) {
            final PieceInfo p = pieces[i];
            res[i] = p.pieceNo;
        }
        return res;
    }

    @SuppressWarnings("synthetic-access")
    int getBestPieceToDownload(final PieceSetViewer availablePieces) {
        final PieceInfo choices[] = new PieceInfo[Settings.RANKER_MAXIMUM_CHOICES];
        int choiceIndex = 0;

        for (int ix = 0; ix < validPieces; ix++) {
            final PieceInfo p = pieces[ix];
            if (choiceIndex > 0 && p.occurrences != choices[0].occurrences) {
                break;
            }
            if (p.downloads < 1 && availablePieces.get(p.pieceNo)) {
                choices[choiceIndex++] = p;
                if (choiceIndex >= choices.length) {
                    break;
                }
            }
        }
        if (choiceIndex < 1) {
            return -1;
        }
        return choices[Globals.rng.nextInt(choiceIndex)].pieceNo;
    }

    @SuppressWarnings("synthetic-access")
    int getBestPieceToDownload(final PieceSet availablePieces,
            final int maximalReplication) {
        final PieceInfo choices[] = new PieceInfo[Settings.RANKER_MAXIMUM_CHOICES];
        int choiceIndex = 0;

        for (int ix = 0; ix < validPieces; ix++) {
            final PieceInfo p = pieces[ix];
            if (p.occurrences > maximalReplication) {
                break;
            }
            if (choiceIndex > 0 && p.occurrences != choices[0].occurrences) {
                break;
            }
            if (p.downloads < 1 && availablePieces.get(p.pieceNo)) {
                choices[choiceIndex++] = p;
                if (choiceIndex >= choices.length) {
                    break;
                }
            }
        }
        if (choiceIndex < 1) {
            return -1;
        }
        return choices[Globals.rng.nextInt(choiceIndex)].pieceNo;
    }

    @SuppressWarnings("synthetic-access")
    int getBestPieceToDownload(final PieceSet availablePieces,
            final OutstandingDownloadingPieceList outstandingPieces,
            final IbisIdentifier peer) {
        final PieceInfo choices[] = new PieceInfo[Settings.RANKER_MAXIMUM_CHOICES];
        int choiceIndex = 0;

        for (int ix = 0; ix < validPieces; ix++) {
            final PieceInfo p = pieces[ix];
            if (choiceIndex > 0 && p.occurrences != choices[0].occurrences) {
                break;
            }
            if (p.downloads < Settings.MAXIMAL_ENDGAME_REPLICATION
                    && availablePieces.get(p.pieceNo)
                    && !outstandingPieces
                            .containsPieceFromPeer(p.pieceNo, peer)) {
                choices[choiceIndex++] = p;
                if (choiceIndex >= choices.length) {
                    break;
                }
            }
        }
        if (choiceIndex < 1) {
            return -1;
        }
        return choices[Globals.rng.nextInt(choiceIndex)].pieceNo;
    }

    @SuppressWarnings("synthetic-access")
    boolean rankingIsSane() {
        boolean sane = true;
        int elm = validPieces;
        for (int i = 0; i < rank.length; i++) {
            final int r = rank[i];
            if (r >= 0) {
                elm--;
                if (pieces[r] == null) {
                    Globals.log.reportInternalError("Piece rank for " + i
                            + "(=" + r + ") points to null entry");
                    sane = false;
                } else if (i != pieces[r].pieceNo) {
                    Globals.log.reportInternalError("Piece rank for " + i
                            + "(=" + r
                            + ") points to piece info for wrong piece "
                            + pieces[r].pieceNo);
                    sane = false;
                }
            }
        }
        if (elm < 0) {
            Globals.log
                    .reportInternalError("Too many valid entries in rank array???");
        } else if (elm > 0) {
            Globals.log.reportInternalError("There are " + elm
                    + " unranked piece entries");
        }
        return sane;
    }

    @SuppressWarnings("synthetic-access")
    void registerDownloadCancel(final int piece) {
        final int ix = rank[piece];
        if (ix >= 0) {
            pieces[ix].registerDownloadCancel();
        }
    }

    @SuppressWarnings("synthetic-access")
    void registerDownloadStart(final int piece) {
        final int ix = rank[piece];
        if (ix >= 0) {
            pieces[ix].registerDownloadStart();
        }
    }

    void registerCompletedPiece(final int piece) {
        removeRank(piece);
    }

    @SuppressWarnings("synthetic-access")
    String buildRankingString() {
        final StringBuffer buf = new StringBuffer();
        int currentRank = -1;
        boolean first = true;
        boolean closeList = false;
        for (int i = 0; i < validPieces; i++) {
            final PieceInfo e = pieces[i];
            if (e.occurrences != currentRank) {
                currentRank = e.occurrences;
                if (closeList) {
                    buf.append(']');
                }
                first = true;
                buf.append(" " + e.occurrences + ":[");
                closeList = true;
            }
            if (first) {
                first = false;
            } else {
                buf.append(',');
            }
            buf.append(e.pieceNo);
            if (hasSuccessor(pieces, validPieces, i)) {
                i++;
                while (hasSuccessor(pieces, validPieces, i)) {
                    i++;
                }
                buf.append('-');
                buf.append(pieces[i].pieceNo);
            }
        }
        if (closeList) {
            buf.append(']');
        }
        return buf.toString();
    }

    @SuppressWarnings("synthetic-access")
    private boolean hasSuccessor(final PieceInfo[] l, final int n, final int i) {
        final int i1 = i + 1;
        return i1 < n && l[i].pieceNo + 1 == l[i1].pieceNo
                && l[i].occurrences == l[i1].occurrences;
    }

    void dumpState() {
        final String ranking = buildRankingString();
        Globals.log.reportProgress("Piece ranking:" + ranking);
    }

    @SuppressWarnings("synthetic-access")
    void removePendingDownload(final int piece) {
        final int ix = rank[piece];
        if (ix >= 0) {
            pieces[ix].registerDownloadCancel();
        }
    }
}
