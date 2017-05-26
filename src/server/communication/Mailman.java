package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Mailman {
    private static final int MAX_SIMULTANEOUS_CONNECTIONS = 128;

    public static final ConcurrentHashMap<NodeInfo, Connection> openConnections = new ConcurrentHashMap<>();
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
        if (nodeInfo.equals(currentNode.getInfo()))
            try {
                throw new Exception("Opening connection to self.");
            } catch (Exception e) {
                e.printStackTrace();
            }

        return isConnectionOpen(nodeInfo)
                ? openConnections.get(nodeInfo)
                : addOpenConnection(new Connection(nodeInfo));
    }

    public static void sendOperation(NodeInfo destination, Operation operation) throws IOException {
        if (operation == null) {
            System.err.println("Received null object to send.");
            return;
        }

        /* If we want to send the operation to the current node, it is equivalent to just running it.
         * Otherwise, send to the correct node as expected. */
        if (destination.equals(currentNode.getInfo()))
            operation.run(currentNode);
        else {
            int attempts = 3;
            while (attempts > 0) {
                try {
                    getOrOpenConnection(destination).sendOperation(operation);
                } catch (Exception e) {
                    e.printStackTrace();
                    attempts--;
                }
            }
        }
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
                new Connection(socket, currentNode, connectionsThreadPool);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Connection addOpenConnection(Connection connection) throws IOException {
        openConnections.put(connection.getNodeInfo(), connection);
        connectionsThreadPool.submit(() -> connection.listen(currentNode));
        return connection;
    }

    public static void connectionClosed(NodeInfo connection) {
        openConnections.remove(connection);
    }

    public static void state() {
        System.out.println(openConnections.toString());
    }
}
