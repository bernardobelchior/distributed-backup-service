package chord;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Node {
    /**
     * chord.Node ID length in bits.
     */
    public static final int NODE_ID_LENGTH = 160;

    /**
     * Key length in bits.
     */
    public static final int KEY_LENGTH = 160;

    private final int port;
    private final NodeInfo self;
    private final FingerTable fingerTable;

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws UnknownHostException, NoSuchAlgorithmException {
        this.port = port;
        fingerTable = new FingerTable();
        self = new NodeInfo(InetAddress.getLocalHost(), port);

        System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + self.getIdAsHex() + ".");
    }

    /**
     * Starts the process of joining an already established network
     *
     * @param bootstrapperNode chord.Node to get information from.
     */
    public void bootstrap(NodeInfo bootstrapperNode) {
        //start the process of joining the
    }
}
