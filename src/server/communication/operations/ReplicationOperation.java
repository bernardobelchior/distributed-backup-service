package server.communication.operations;

import server.chord.Node;

import java.math.BigInteger;

public class ReplicationOperation implements Operation{
    private final BigInteger key;
    private final byte[] value;

    public ReplicationOperation(BigInteger key, byte[] value)  {
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.backup(key, value);
    }
}
