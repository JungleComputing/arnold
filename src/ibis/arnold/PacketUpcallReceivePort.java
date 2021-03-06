package ibis.arnold;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;

import java.io.IOException;

/**
 * A Receive port for packet reception.
 * 
 * @author Kees van Reeuwijk
 * 
 */
class PacketUpcallReceivePort implements MessageUpcall {
    static final PortType portType = new PortType(
            PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
            PortType.CONNECTION_MANY_TO_ONE, PortType.RECEIVE_AUTO_UPCALLS,
            PortType.RECEIVE_EXPLICIT);

    private final ReceivePort port;

    private final PacketReceiveListener listener;

    /**
     * Constructs a new PacketSendPort.
     * 
     * @param ibis
     *            The Ibis the port will belong to.
     * @param name
     *            The name of the port.
     * @throws IOException
     */
    PacketUpcallReceivePort(final Ibis ibis, final String name,
            final PacketReceiveListener listener) throws IOException {
        this.listener = listener;
        port = ibis.createReceivePort(portType, name, this);
    }

    /**
     * Handle the upcall of the ipl port. Only public because the interface
     * requires it.
     * 
     * @param msg
     *            The message to handle.
     * @throws IOException
     *             Thrown if for some reason the given message could not be
     *             read.
     */
    @Override
    public void upcall(final ReadMessage msg) throws IOException {
        Message data;
        try {
            data = (Message) msg.readObject();
        } catch (final ClassNotFoundException e) {
            Globals.log
                    .reportInternalError("Cannot read message in upcall: class not found: "
                            + e.getLocalizedMessage());
            return;
        }
        // msg.finish();
        data.source = msg.origin().ibisIdentifier();
        listener.messageReceived(data);
    }

    /** Enable this port. */
    protected void enable() {
        port.enableMessageUpcalls();
        port.enableConnections();
    }
}
