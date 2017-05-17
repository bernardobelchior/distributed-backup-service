package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;


public class RequestPredecessorResultOperation implements Operation {

    private NodeInfo predecessor;

    public RequestPredecessorResultOperation(NodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    @Override
    public void run(Node currentNode) {
        FingerTable fingerTable = currentNode.getFingerTable();
        int predecessorId = predecessor.getId();
        int successorId = fingerTable.getSuccessor().getId();
        int currentId = currentNode.getInfo().getId();

        if (currentNode.getFingerTable().between(predecessorId, successorId, currentId)) {
            System.out.println("Setting predecessor to ID " + predecessorId + ".");
            currentNode.finishPredecessorRequest(predecessor);
        }
    }

}
