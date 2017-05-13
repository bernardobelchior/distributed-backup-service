package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;

public class LookupOperation implements Operation {
    private BigInteger key;
    private NodeInfo origin;
    private boolean isLastHop = false;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        try {
            System.out.println("Running lookup operation.");
            if (isLastHop || currentNode.emptyFingerTable())
                Mailman.sendObject(origin, new LookupResultOperation(currentNode.getInfo(), key));

            if (currentNode.keyBelongsToSuccessor(key))
                isLastHop = true;

            currentNode.forwardToNextBestSuccessor(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigInteger getKey() {
        return key;
    }
}
