package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

public class LookupResultOperation extends Operation {
    private final BigInteger key;

    LookupResultOperation(NodeInfo origin, BigInteger key) {
        super(origin);
        this.key = key;
    }

    /**
     * This Operation finishes the Lookup Operation and removes it from the operation manager.
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.onLookupFinished(key, origin);
    }

}
