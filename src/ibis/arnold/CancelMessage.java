package ibis.arnold;

class CancelMessage extends SmallMessage {
    private static final long serialVersionUID = 1L;

    final Chunk chunk;

    CancelMessage(final Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public String toString() {
        return "CancelMessage[" + chunk + ']';
    }
}
