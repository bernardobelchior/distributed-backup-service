package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.math.BigInteger;

public class GetResultOperation extends Operation {
    private final BigInteger key;
    private final byte[] value;

    GetResultOperation(NodeInfo origin, BigInteger key, byte[] value) {
        super(origin);
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.ongoingGets.operationFinished(key, value);
    }
}
