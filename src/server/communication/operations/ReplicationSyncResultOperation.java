package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;
import java.util.HashSet;

public class ReplicationSyncResultOperation extends Operation {
    private final HashSet<BigInteger> keysToDelete;

    public ReplicationSyncResultOperation(NodeInfo origin, HashSet<BigInteger> keysToDelete) {
        super(origin);
        this.keysToDelete = keysToDelete;
    }

    /**
     * This Operation finishes the Replication Sync Operation and removes it from the operation manager.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.updateReplicas(origin, keysToDelete);
    }
}
