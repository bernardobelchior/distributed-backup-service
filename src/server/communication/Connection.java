package server.communication;


import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.operations.Operation;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


import encryptUtils.KeyEncryption.genkeys;
import encryptUtils.KeyEncryption.encryptMessage;
import encryptUtils.KeyEncryption.decrypt;
import encryptUtils.KeyEncryption.obtainPublicKey;
import encryptUtils.KeyEncryption.obtainPrivateKey;

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
		//Não sei qual diretorio por
		//genkeys(pathpapublica,pathpaprivada);
		
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
				//Still needs decrypting
				
                ((Operation) inputStream.readObject()).run(currentNode);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
