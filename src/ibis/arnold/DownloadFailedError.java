package ibis.arnold;

class DownloadFailedError extends Error {
    private static final long serialVersionUID = -1067888231648851673L;

    DownloadFailedError(final String message, final Throwable cause) {
        super(message, cause);
    }

}
