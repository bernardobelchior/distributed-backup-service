package server.communication;


import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.operations.PingOperation;
import server.communication.operations.PingResultOperation;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;
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
        System.out.println("Sending Operation");

        try {
            synchronized (objectOutputStream) {
                objectOutputStream.writeObject(operation);
                objectOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("Sent Operation");
    }

    public synchronized void waitForAuthentication(Node self) {
        try {
            Operation operation;
            System.out.println("WaitForAuth :: received");
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

    private synchronized void closeConnection() {
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

    CompletableFuture<Void> ping(NodeInfo origin, NodeInfo destination) throws IOException {
        CompletableFuture<Void> ping = Mailman.ongoingPings.putIfAbsent(destination.getId());

        if (ping != null)
            return ping;

        ping = Mailman.ongoingPings.get(destination.getId());

        System.out.println("Pinging Node " + destination.getId());
        synchronized (objectOutputStream) {
            objectOutputStream.writeObject(new PingOperation(origin));
            objectOutputStream.flush();
        }
        System.out.println("Pinged Node " + destination.getId());

        return ping;
    }

    void pong(Node node, NodeInfo currentNode) throws IOException {

        System.out.println("ponging node " + destination.getId());

        synchronized (objectOutputStream) {
            PingResultOperation pingResultOperation = new PingResultOperation(currentNode);
            objectOutputStream.writeObject(pingResultOperation);
            objectOutputStream.flush();
        }
        System.out.println("ponged node " + destination.getId());
    }
}
