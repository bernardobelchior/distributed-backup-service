package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

public class InsertResultOperation extends Operation {
    private final BigInteger key;
    private final boolean successful;

    InsertResultOperation(NodeInfo origin, BigInteger key, boolean successful) {
        super(origin);
        this.key = key;
        this.successful = successful;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.ongoingInsertions.operationFinished(key, successful);
    }
}
