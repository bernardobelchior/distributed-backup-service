package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;

import java.io.IOException;
import java.math.BigInteger;

public class LookupResultOperation implements Operation {
    private BigInteger key;
    private NodeInfo origin;

    public LookupResultOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        try {
            Mailman.sendObject(origin, currentNode.getValue(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigInteger getKey() {
        return key;
    }
}
