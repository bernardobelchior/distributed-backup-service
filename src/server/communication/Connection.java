package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.operations.Operation;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;

public class Connection {
    private final SSLSocket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private NodeInfo nodeInfo;

    Connection(NodeInfo nodeInfo) throws IOException {
        this.nodeInfo = nodeInfo;
        socket = (SSLSocket) SSLSocketFactory.getDefault().
                createSocket(nodeInfo.getAddress(), nodeInfo.getPort());

        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    Connection(SSLSocket socket, Node currentNode, ExecutorService connectionsThreadPool) throws IOException {
        this.socket = socket;
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        connectionsThreadPool.submit(() -> waitForAuthentication(currentNode));
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public void sendOperation(Operation operation) throws IOException {
        synchronized (outputStream) {
            outputStream.writeObject(operation);
        }
    }

    public synchronized void waitForAuthentication(Node currentNode) {
        try {
            Operation operation = ((Operation) inputStream.readObject());
            this.nodeInfo = operation.getOrigin();
            Mailman.addOpenConnection(this);
            operation.run(currentNode);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen(Node currentNode) {
        while (true) {
            try {
                synchronized (this) {
                    ((Operation) inputStream.readObject()).run(currentNode);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                currentNode.informAboutFailure(nodeInfo);
                closeConnection();
                return;
            }
        }
    }

    private synchronized void closeConnection() {
        Mailman.connectionClosed(nodeInfo);

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Unable to close socket");
        }
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
