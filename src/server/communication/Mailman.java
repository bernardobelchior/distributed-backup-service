package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.operations.Operation;

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
    private static final ExecutorService connectionsThreadPool = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_CONNECTIONS);

    private static Node currentNode;

    public static void init(Node currentNode, int port) {
        Mailman.currentNode = currentNode;
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
        if (object == null) {
            System.err.println("Received null object to send.");
            return;
        }

        if(nodeInfo.getId() == currentNode.getInfo().getId())
            System.err.println("WARNING! Trying to send object to self! Looking for object " + ((Operation) object).getKey());

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
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    private static Connection addOpenConnection(Connection connection) throws IOException {
        openConnections.put(connection.getNodeInfo(), connection);
        connectionsThreadPool.submit(() -> connection.listen(currentNode));
        return connection;
    }

    public static void connectionClosed(NodeInfo connection) {
        openConnections.remove(connection);
    }
}
