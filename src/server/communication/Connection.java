package server.communication;

import server.chord.NodeInfo;
import server.dht.DistributedHashTable;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Connection {
    private boolean open;
    private SSLSocket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private NodeInfo nodeInfo;

    Connection(NodeInfo nodeInfo) throws IOException {
        this.nodeInfo = nodeInfo;
        socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(nodeInfo.getAddress(), nodeInfo.getPort());
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());

    }

    Connection(NodeInfo nodeInfo, SSLSocket socket) {
        this.nodeInfo = nodeInfo;
        this.socket = socket;
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public void sendObject(Object object) throws IOException {
        outputStream.writeObject(object);
    }

    public void listen(DistributedHashTable<?> dht) {
        try {
            ((Operation) inputStream.readObject()).run(dht);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
