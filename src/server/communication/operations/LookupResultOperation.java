package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

public class LookupResultOperation extends Operation {
    private final NodeInfo resultNode;
    private final BigInteger key;

    public LookupResultOperation(NodeInfo origin, NodeInfo resultNode, BigInteger key) {
        super(origin);
        this.resultNode = resultNode;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Received pong from node" + origin.getId());
        currentNode.onLookupFinished(key, resultNode);
    }

}
