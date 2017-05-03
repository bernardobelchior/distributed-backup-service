package server.chord;

import server.Server;
import server.communication.MessageOperation;
import server.communication.Operation;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static server.chord.FingerTable.between;

public class Node {
    public static final int MAX_NODES = 128;

    private final NodeInfo self;
    private NodeInfo predecessor;
    private final FingerTable fingerTable;

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws IOException, NoSuchAlgorithmException {
        self = new NodeInfo(InetAddress.getLocalHost(), port);
        fingerTable = new FingerTable(self);
        predecessor = self;

        System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + Integer.toUnsignedString(self.getId()) + ".");
    }

    /**
     * Starts the process of joining an already established network
     *
     * @param bootstrapperNode server.chord.Node to get information from.
     */
    public void bootstrap(NodeInfo bootstrapperNode) throws IOException {
        SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(bootstrapperNode.getAddress(), bootstrapperNode.getPort());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
        objectOutputStream.writeObject(new MessageOperation());
        objectOutputStream.flush();
    }

    public void get(BigInteger key) {
        fingerTable.lookup(key);
    }

    public boolean inRange(BigInteger key) {
        return between(predecessor, self, key);
    }
}
