package server.communication;

import server.chord.Node;
import server.chord.NodeInfo;

import java.io.IOException;
import java.math.BigInteger;

public class LookupOperation implements Operation {
    private BigInteger key;
    private NodeInfo origin;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        try {
            if (currentNode.keyBelongsToSuccessor(key)) {
                Mailman.sendObject(currentNode.getSuccessor(), new LookupReturnOperation(origin, key));
            } else {
                currentNode.forwardToNextBestSuccessor(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigInteger getKey() {
        return key;
    }
}
