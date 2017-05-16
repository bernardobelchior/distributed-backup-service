package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;

/**
 * Created by epassos on 5/15/17.
 */
public class RequestPredecessorResultOperation implements Operation {

    NodeInfo predecessor;

    public RequestPredecessorResultOperation(NodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    @Override
    public void run(Node currentNode) {
        FingerTable fingerTable = currentNode.getFingerTable();
        int predecessorId = predecessor.getId();
        int successorId = fingerTable.getSuccessor().getId();
        int currentId = currentNode.getInfo().getId();

        System.out.format("Checking if %d is between %d and %d",currentId,predecessorId,successorId);
        if (currentNode.getFingerTable().between(predecessorId, successorId, currentId)) {
            System.out.println("Setting predecessor to ID " + predecessorId);
            currentNode.finishPredecessorRequest(predecessor);
        }
    }

    @Override
    public String getKey() {
        return null;
    }
}
