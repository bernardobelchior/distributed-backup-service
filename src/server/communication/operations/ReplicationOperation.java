package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

public class ReplicationOperation extends Operation {
    private final BigInteger key;
    private final byte[] value;

    public ReplicationOperation(NodeInfo self, BigInteger key, byte[] value) {
        super(self);
        this.key = key;
        this.value = value;
    }

    /**
     * This Operation stores the replicas with the value with the key in the current node.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.storeReplica(origin, key, value);
    }
}
