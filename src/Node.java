import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Node {
    private int port;
    private byte[] id;

    /**
     * @param port Port to start the service in
     */
    Node(int port) {
        this.port = port;
    }

    public void start() throws UnknownHostException, NoSuchAlgorithmException {
        generateId();

        System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + getIdHex() + ".");
    }

    private void generateId() throws NoSuchAlgorithmException, UnknownHostException {
        byte[] localAddress = InetAddress.getLocalHost().getAddress();

        byte[] idGenerator = Arrays.copyOf(localAddress, localAddress.length + 4);

        idGenerator[4] = (byte) (port >> 24);
        idGenerator[5] = (byte) (port >> 16);
        idGenerator[6] = (byte) (port >> 8);
        idGenerator[7] = (byte) port;

        this.id = Utils.hash(idGenerator);
    }

    public String getIdHex() {
        return DatatypeConverter.printHexBinary(id);
    }

    public void bootstrap(String address, int port) throws IOException {
        NodeConnection node = new NodeConnection(address, port);
    }
}
