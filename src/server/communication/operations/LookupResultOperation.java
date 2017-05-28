package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

import static server.chord.Node.MAX_NODES;

public class LookupResultOperation extends Operation {
    private final BigInteger key;

    LookupResultOperation(NodeInfo origin, BigInteger key) {
        super(origin);
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Result of key " + Integer.remainderUnsigned(key.intValue(), MAX_NODES) + " resolved as " + origin.getId());
        currentNode.onLookupFinished(key, origin);
    }

}
