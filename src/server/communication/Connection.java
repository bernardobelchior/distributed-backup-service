package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.operations.Operation;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Connection {
    private final SSLSocket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private final NodeInfo nodeInfo;

    Connection(NodeInfo nodeInfo) throws IOException {
        this(
                nodeInfo,
                (SSLSocket) SSLSocketFactory.getDefault().
                        createSocket(nodeInfo.getAddress(), nodeInfo.getPort()));
    }

    Connection(NodeInfo nodeInfo, SSLSocket socket) throws IOException {
        this.nodeInfo = nodeInfo;
        this.socket = socket;
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public void sendOperation(Operation operation) throws IOException {
        synchronized (outputStream) {
            outputStream.writeObject(operation);
        }
    }

    public void listen(Node currentNode) {
        while (true) {
            try {
                ((Operation) inputStream.readObject()).run(currentNode);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Connection to node " + nodeInfo.getId() + " closed.");
                closeConnection();
                return;
            }
        }
    }

    private void closeConnection() {
        Mailman.connectionClosed(nodeInfo);

        try {
            socket.close();
        } catch (IOException e1) {
            System.err.println("Unable to close socket");
            e1.printStackTrace();
        }
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
