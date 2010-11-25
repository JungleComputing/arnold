package ibis.arnold;

import ibis.ipl.IbisIdentifier;

import java.io.Serializable;

/**
 * The superclass of all messages that are exchanged in the simulator.
 * 
 * @author Kees van Reeuwijk
 * 
 */
abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    protected transient IbisIdentifier source;

    transient long arrivalTime;

    @Override
    public String toString() {
        return Utils.toStringClassScalars(this);
    }
}
