package server.communication;

import server.chord.NodeInfo;
import server.dht.DistributedHashTable;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Mailman {
    private static final int MAX_SIMULTANEOUS_CONNECTIONS = 128;

    private static final ConcurrentHashMap<NodeInfo, Connection> openConnections = new ConcurrentHashMap<>();
    private static final ExecutorService socketPool = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_CONNECTIONS);

    private static DistributedHashTable<?> dht;

    public static void init(int port, DistributedHashTable<?> dht) {
        Mailman.dht = dht;
        new Thread(() -> listenForConnections(port)).start();
    }

    private static boolean isConnectionOpen(NodeInfo nodeInfo) {
        return openConnections.containsKey(nodeInfo) && openConnections.get(nodeInfo).isOpen();
    }

    private static Connection getOrOpenConnection(NodeInfo nodeInfo) throws IOException {
        return isConnectionOpen(nodeInfo)
                ? openConnections.get(nodeInfo)
                : addOpenConnection(new Connection(nodeInfo));
    }

    public static void sendObject(NodeInfo nodeInfo, Object object) throws IOException {
        getOrOpenConnection(nodeInfo).sendObject(object);
    }

    public static void listenForConnections(int port) {
        SSLServerSocket serverSocket;
        try {
            serverSocket = (SSLServerSocket)
                    SSLServerSocketFactory.getDefault().createServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error creating server socket.");
            e.printStackTrace();
            return;
        }

        serverSocket.setNeedClientAuth(true);

        while (true) {
            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                NodeInfo nodeInfo = new NodeInfo(socket.getInetAddress(), socket.getPort());

                Connection connection = new Connection(nodeInfo, socket);
                addOpenConnection(connection);
            } catch (IOException e) {
                System.err.println("Error accepting socket.");
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public static Connection addOpenConnection(Connection connection) throws IOException {
        openConnections.put(connection.getNodeInfo(), connection);
        socketPool.submit(() -> connection.listen(dht));
        return connection;
    }
}
