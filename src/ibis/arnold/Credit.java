package ibis.arnold;

/**
 * The credit of the system.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class Credit {
    private double value = 0;
    private long serialNumber = 0;

    synchronized void add(final double v, final String reason) {
        this.value += v;
        serialNumber++;
        if (Settings.TraceCredits) {
            Globals.log.reportProgress("Added " + v + " to credit. new value="
                    + value + " serial=" + serialNumber + " (" + reason + ")");
        }
    }

    synchronized CreditValue getValue() {
        return new CreditValue(value, serialNumber);
    }
}
