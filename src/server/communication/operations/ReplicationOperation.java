package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.math.BigInteger;

public class ReplicationOperation extends Operation {
    private final BigInteger key;
    private final byte[] value;

    public ReplicationOperation(NodeInfo self, BigInteger key, byte[] value) {
        super(self);
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.backup(key, value);
    }
}
