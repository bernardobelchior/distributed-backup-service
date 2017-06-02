package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static server.chord.Node.OPERATION_MAX_FAILED_ATTEMPTS;

public class Mailman {
    private static final int MAX_SIMULTANEOUS_CONNECTIONS = 128;

    private static final ConcurrentHashMap<NodeInfo, Connection> openConnections = new ConcurrentHashMap<>();
    private static final ExecutorService connectionsThreadPool = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_CONNECTIONS);
    private static Node currentNode;

    /**
     * Initiates the listening for Connections.
     *
     * @param currentNode
     * @param address
     */
    public static void init(Node currentNode, InetAddress address, int port) {
        Mailman.currentNode = currentNode;
        new Thread(() -> listenForConnections(address, port)).start();
    }

    /**
     * Checks if the Connection is open.
     *
     * @param nodeInfo
     * @return
     */
    private static boolean isConnectionOpen(NodeInfo nodeInfo) {
        return openConnections.containsKey(nodeInfo) && openConnections.get(nodeInfo).isOpen();
    }

    /**
     * Opens connection to give node.
     *
     * @param nodeInfo
     * @return
     * @throws IOException
     */
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

    /**
     * Sends the given Operation to the given destination.
     *
     * @param destination
     * @param operation
     * @throws IOException
     */
    public static void sendOperation(NodeInfo destination, Operation operation) throws IOException {
        /* If we want to send the operation to the current node, it is equivalent to just running it.
         * Otherwise, send to the correct node as expected. */
        if (destination.equals(currentNode.getInfo())) {
            operation.run(currentNode);
        } else {
            int attempts = OPERATION_MAX_FAILED_ATTEMPTS;
            while (attempts > 0) {
                try {
                    getOrOpenConnection(destination).sendOperation(operation);
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    openConnections.remove(destination);
                    addOpenConnection(new Connection(destination));
                    attempts--;
                    if (attempts < 1)
                        throw e;
                }
            }
        }
    }


    /**
     * Listening for connetions on the given port.
     *
     * @param port
     */
    private static void listenForConnections(InetAddress address, int port) {
        SSLServerSocket serverSocket;
        try {
            serverSocket = (SSLServerSocket)
                    SSLServerSocketFactory.getDefault().createServerSocket(port, 20, address);
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

    /**
     * Add the given Operation to the Open Connections Hash Map.
     *
     * @param connection
     * @return
     * @throws IOException
     */
    static Connection addOpenConnection(Connection connection) throws IOException {
        Connection previousConnection = openConnections.put(connection.getNodeInfo(), connection);
        if (previousConnection != null)
            previousConnection.closeConnection();

        connectionsThreadPool.submit(() -> connection.listen(currentNode));
        return connection;
    }

    /**
     * Remove the given Operation from the Open Connections Hash Map.
     *
     * @param connection
     */
    static void connectionClosed(NodeInfo connection) {
        openConnections.remove(connection);
    }
}
