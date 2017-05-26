package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;
import java.util.HashSet;

public class ReplicationSyncOperation extends Operation {

    private final HashSet<BigInteger> keys;

    public ReplicationSyncOperation(NodeInfo origin, HashSet<BigInteger> keys) {
        super(origin);
        this.keys = keys;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.synchronizeReplicas(origin, keys);
    }
}
