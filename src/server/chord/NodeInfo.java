package server.chord;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static server.chord.Node.MAX_NODES;
import static server.utils.Utils.hash;

public class NodeInfo implements Serializable {

    private final int id;
    private final InetAddress address;
    private final int port;

    public NodeInfo(InetAddress address, int port) throws NoSuchAlgorithmException {
        this.address = address;
        this.port = port;
        this.id = generateId(address.getAddress(), port);
    }

    private static int generateId(byte[] address, int port) throws NoSuchAlgorithmException {
        byte[] idGenerator = Arrays.copyOf(address, address.length + 4);

        idGenerator[4] = (byte) (port >> 24);
        idGenerator[5] = (byte) (port >> 16);
        idGenerator[6] = (byte) (port >> 8);
        idGenerator[7] = (byte) port;


        BigInteger id = new BigInteger(hash(idGenerator));
        return Integer.remainderUnsigned(id.intValue(), MAX_NODES);
    }

    public int getId() {
        return id;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "ID: " + id + "  IP Address: " + address.getHostAddress() + "  Port: " + port;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NodeInfo && ((NodeInfo) o).id == id;
    }
}
