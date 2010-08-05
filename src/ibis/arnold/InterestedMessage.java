package ibis.arnold;

class InterestedMessage extends SmallMessage {
    private static final long serialVersionUID = 1L;
    final boolean flag;

    InterestedMessage(final boolean flag) {
        this.flag = flag;
    }
}
