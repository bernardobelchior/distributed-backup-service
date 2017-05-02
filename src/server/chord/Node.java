package server.chord;

import server.dht.Message;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {
    public static final int MAX_NODES = 128;
    private final int MAX_SIMULTANEOUS_CONNECTIONS = 5;
    private final int STORED_SUCCESSORS = 3;

    private final ExecutorService socketPool;
    private final NodeInfo self;
    private NodeInfo predecessor;
    private final NodeInfo[] sucessors = new NodeInfo[STORED_SUCCESSORS];
    private final FingerTable fingerTable;

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws IOException, NoSuchAlgorithmException {
        self = new NodeInfo(InetAddress.getLocalHost(), port);
        socketPool = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_CONNECTIONS);
        fingerTable = new FingerTable(self);
        predecessor = self;

        for (int i = 0; i < sucessors.length; i++) {
            sucessors[i] = self;
        }

        System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + Integer.toUnsignedString(self.getId()) + ".");
        new Thread(this::startAcceptingSockets).start();
    }

    /**
     * Starts the process of joining an already established network
     *
     * @param bootstrapperNode server.chord.Node to get information from.
     */
    public void bootstrap(NodeInfo bootstrapperNode) throws IOException {
        SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(bootstrapperNode.getAddress(), bootstrapperNode.getPort());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
        objectOutputStream.writeObject(new Message());
        objectOutputStream.flush();
    }

    private void startAcceptingSockets() {
        SSLServerSocket serverSocket;
        try {
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(self.getPort());
        } catch (IOException e) {
            System.err.println("Error creating server socket.");
            e.printStackTrace();
            return;
        }

        serverSocket.setNeedClientAuth(true);

        while (true) {
            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                socketPool.submit(() -> receiveMessage(socket));
            } catch (IOException e) {
                System.err.println("Error accepting socket.");
                e.printStackTrace();
            }
        }
    }

    private void receiveMessage(SSLSocket socket) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            try {
                Message message = (Message) objectInputStream.readObject();
                System.out.println(message);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds the node corresponding to the given key
     *
     * @param key key to search
     * @return NodeInfo belonging to the node responsible for the key
     */
    private NodeInfo lookup(int key, int lastNode) {
        NodeInfo destination;
        int selfId = self.getId();

        if (between(selfId, sucessors[0].getId(), key)) {
            destination = sucessors[0];
        } else destination = fingerTable.lookup(selfId, key);

        return destination;
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public static boolean between(int lower, int upper, int key) {
        if (lower < upper)
            return key > lower && key <= upper;

        else return key > lower || key <= upper;
    }

}
