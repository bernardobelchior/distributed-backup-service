package chord;

import static utils.Utils.*;

import javax.xml.bind.DatatypeConverter;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class NodeInfo {
    private final byte[] id;
    private final InetAddress address;
    private final int port;

    public NodeInfo(InetAddress address, int port) throws NoSuchAlgorithmException {
        this.address = address;
        this.port = port;
        this.id = generateId(address.getAddress(), port);
    }

    private static byte[] generateId(byte[] address, int port) throws NoSuchAlgorithmException {
        byte[] idGenerator = Arrays.copyOf(address, address.length + 4);

        idGenerator[4] = (byte) (port >> 24);
        idGenerator[5] = (byte) (port >> 16);
        idGenerator[6] = (byte) (port >> 8);
        idGenerator[7] = (byte) port;

        return hash(idGenerator);
    }

    public String getIdAsHex() {
        return DatatypeConverter.printHexBinary(id);
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
