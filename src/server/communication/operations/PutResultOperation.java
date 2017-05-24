package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.math.BigInteger;

public class PutResultOperation extends Operation {
    private final BigInteger key;
    private final boolean successful;

    PutResultOperation(NodeInfo origin, BigInteger key, boolean successful) {
        super(origin);
        this.key = key;
        this.successful = successful;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.ongoingPuts.operationFinished(key, successful);
    }
}
