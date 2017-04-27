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
    private final int MAX_SIMULTANEOUS_CONNECTIONS = 5;

    private final ExecutorService socketPool;
    private final int port;
    private final NodeInfo self;
    private final FingerTable fingerTable;

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws IOException, NoSuchAlgorithmException {
        this.port = port;
        fingerTable = new FingerTable();
        self = new NodeInfo(InetAddress.getLocalHost(), port);
        socketPool = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_CONNECTIONS);

        System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + self.getIdAsHex() + ".");
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
                socketPool.submit(() -> connectionOpened(socket));
            } catch (IOException e) {
                System.err.println("Error accepting socket.");
                e.printStackTrace();
            }
        }
    }

    private void connectionOpened(SSLSocket socket) {
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
}
