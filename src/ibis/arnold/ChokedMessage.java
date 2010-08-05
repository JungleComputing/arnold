package ibis.arnold;

class ChokedMessage extends SmallMessage {
    private static final long serialVersionUID = 1L;

    protected final boolean flag;
    private final String reason;

    ChokedMessage(final boolean flag, final String reason) {
        this.flag = flag;
        this.reason = reason;
    }
}
