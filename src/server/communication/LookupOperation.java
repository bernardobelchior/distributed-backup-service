package server.communication;

import server.chord.NodeInfo;
import server.dht.DistributedHashTable;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;

import static server.Server.listenForMessages;

public class LookupOperation<T> implements Operation<T> {
    private BigInteger key;
    private NodeInfo origin;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        this.key = key;
    }

    @Override
    public void run(DistributedHashTable<T> dht) {
        if (dht.inRangeOfCurrentNode(key)) {
            SSLSocket sslSocket;
            try {
                sslSocket = (SSLSocket)
                        SSLSocketFactory.getDefault().createSocket(
                                origin.getAddress(),
                                origin.getPort());
            } catch (IOException e) {
                System.err.println("Could not open connection to origin node in Lookup for key " + DatatypeConverter.printHexBinary(key.toByteArray()));
                return;
            }

            listenForMessages(sslSocket, dht);
        }
    }
}
