package ibis.arnold;

class RequestMessage extends SmallMessage {
    private static final long serialVersionUID = 1L;

    final CreditValue credit;
    final Chunk chunk;

    RequestMessage(final CreditValue credit, final Chunk chunk) {
        this.credit = credit;
        this.chunk = chunk;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RequestMessage)) {
            return false;
        }
        final RequestMessage other = (RequestMessage) obj;
        return this.source.equals(other.source)
                && this.chunk.equals(other.chunk);
    }

    @Override
    public String toString() {
        return "RequestMessage[" + chunk + "]";
    }

    @Override
    public int hashCode() {
        return chunk.hashCode() ^ source.hashCode();
    }
}
