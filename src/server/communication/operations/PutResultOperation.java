package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.math.BigInteger;

public class PutResultOperation<T> implements Operation<T> {
    private final BigInteger key;
    private final boolean successful;
    private final NodeInfo origin;

    PutResultOperation(NodeInfo origin, BigInteger key, boolean successful) {
        this.origin = origin;
        this.key = key;
        this.successful = successful;
    }
    @Override
    public void run(Node<T> currentNode) {
        currentNode.finishedPut(key, successful);
    }
}
