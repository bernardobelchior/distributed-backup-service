package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

public class GetResultOperation extends Operation {
    private final BigInteger key;
    private final byte[] value;

    GetResultOperation(NodeInfo origin, BigInteger key, byte[] value) {
        super(origin);
        this.key = key;
        this.value = value;
    }

    /**
     *This Operation finishes the Get Operation and removes it from the operation manager.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.ongoingGets.operationFinished(key, value);
    }
}
