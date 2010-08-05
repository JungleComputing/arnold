package ibis.arnold;

import java.io.Serializable;

/**
 * A credit value.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class CreditValue implements Serializable {
    private static final long serialVersionUID = 1L;
    final double value;
    private final long sequence;

    CreditValue(final double value, final long sequence) {
        this.value = value;
        this.sequence = sequence;
    }

    CreditValue update(final CreditValue credit) {
        if (credit.sequence > this.sequence) {
            return credit;
        }
        return this;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}
