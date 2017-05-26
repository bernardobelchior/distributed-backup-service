package server.communication;


import server.chord.Node;
import server.chord.NodeInfo;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;


public class Connection {
    private final SSLSocket socket;
    private final ObjectOutputStream objectOutputStream;
    private final ObjectInputStream objectInputStream;
    private NodeInfo destination;

    Connection(NodeInfo destination) throws IOException {
        this.destination = destination;
        socket = (SSLSocket) SSLSocketFactory.getDefault().
                createSocket(destination.getAddress(), destination.getPort());

        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    Connection(SSLSocket socket, Node currentNode, ExecutorService connectionsThreadPool) throws IOException {
        this.socket = socket;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        connectionsThreadPool.submit(() -> waitForAuthentication(currentNode));
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public void sendOperation(Operation operation) throws IOException {
        try {
            synchronized (objectOutputStream) {
                objectOutputStream.writeObject(operation);
                objectOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void waitForAuthentication(Node self) {
        try {
            Operation operation;
            operation = ((Operation) objectInputStream.readObject());

            this.destination = operation.getOrigin();
            Mailman.addOpenConnection(this);
            operation.run(self);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            self.informAboutFailure(destination);
            closeConnection();
        }
    }

    public void listen(Node self) {
        while (true) {
            try {
                ((Operation) objectInputStream.readObject()).run(self);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                self.informAboutFailure(destination);
                closeConnection();
                return;
            }
        }
    }

    private void closeConnection() {
        Mailman.connectionClosed(destination);

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Unable to close socket");
        }
    }

    NodeInfo getNodeInfo() {
        return destination;
    }
}
