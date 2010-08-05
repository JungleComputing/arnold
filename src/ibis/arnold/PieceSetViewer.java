package ibis.arnold;

interface PieceSetViewer {

    int cardinality();

    boolean isEmpty();

    boolean intersects(PieceSet unrequestedMissingPieces);

    boolean get(int i);

}
