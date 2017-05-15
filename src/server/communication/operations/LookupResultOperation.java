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
        System.out.println("Lookup operation for key " + key + " resolved as " + resultNode.getId());
        currentNode.finishedLookup(key, resultNode);
        System.out.println("Done");
    }

    @Override
    public String getKey() {
        return String.valueOf(key);
    }
}
