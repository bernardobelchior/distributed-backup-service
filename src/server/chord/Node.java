package server.chord;

import server.communication.LookupOperation;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

import static server.chord.FingerTable.between;

public class Node {
    public static final int MAX_NODES = 128;

    private final NodeInfo self;
    private NodeInfo predecessor;
    private final FingerTable fingerTable;

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws IOException, NoSuchAlgorithmException {
        self = new NodeInfo(InetAddress.getLocalHost(), port);
        fingerTable = new FingerTable(self);
        predecessor = self;

        System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + Integer.toUnsignedString(self.getId()) + ".");
    }

    public void lookup(BigInteger key) throws IOException {
        lookup(key, new LookupOperation<>(self, key));
    }

    public void lookup(BigInteger key, LookupOperation<?> lookupOperation) throws IOException {
        NodeInfo bestNextNode = fingerTable.getBestNextNode(key);
        Mailman.sendObject(bestNextNode, lookupOperation);

    }

    public boolean inRange(BigInteger key) {
        return between(predecessor, self, key);
    }

    public NodeInfo getInfo() {
        return self;
    }
}
