package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.math.BigInteger;

public class LookupResultOperation implements Operation {
    private final NodeInfo resultNode;
    private final BigInteger key;

    public LookupResultOperation(NodeInfo resultNode, BigInteger key) {
        this.resultNode = resultNode;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Running lookup result operation.");
        currentNode.finishedLookup(key, resultNode);
    }
}
