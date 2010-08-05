package ibis.arnold;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.server.Server;

import java.io.IOException;
import java.util.BitSet;
import java.util.Properties;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Tests for Ibis.
 * 
 * @author Kees van Reeuwijk.
 */
public class IbisSerializationTests extends TestCase implements MessageUpcall {
    private static final IbisCapabilities ibisCapabilities = new IbisCapabilities();
    static final PortType portType = new PortType(
            PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
            PortType.CONNECTION_ONE_TO_ONE, PortType.RECEIVE_AUTO_UPCALLS);
    static final String receivePortName = "port";

    static final class MyBitSet extends BitSet {
        private static final long serialVersionUID = 892637456126372282L;
    }

    /**
     * @throws Exception
     *             Thrown if some problem occurs.
     * 
     */
    @Test
    public void testUtils() throws Exception {
        final Properties serverProperties = new Properties();
        final Server server = new Server(serverProperties);
        final Properties ibisProperties = new Properties();
        ibisProperties.setProperty("ibis.pool.name", "test-pool-name");
        ibisProperties.setProperty("ibis.server.address", server.getAddress());
        final Ibis ibis = IbisFactory.createIbis(ibisCapabilities,
                ibisProperties, true, null, portType);
        final SendPort sendPort = ibis.createSendPort(portType);
        final ReceivePort receivePort = ibis.createReceivePort(portType,
                receivePortName, this);
        receivePort.enableConnections();
        sendPort.connect(ibis.identifier(), receivePortName);
        WriteMessage msg = null;
        final MyBitSet payload = new MyBitSet();
        payload.set(5);
        payload.set(12);
        try {
            msg = sendPort.newMessage();
            msg.writeObject(payload);
        } finally {
            if (msg != null) {
                msg.finish();
            }
        }
        sendPort.close();
        ibis.end();
        server.end(1000);
    }

    @Override
    public void upcall(final ReadMessage msg) throws IOException,
            ClassNotFoundException {
        final Object obj = msg.readObject();
        System.out.println("Received object " + obj);
    }

}
