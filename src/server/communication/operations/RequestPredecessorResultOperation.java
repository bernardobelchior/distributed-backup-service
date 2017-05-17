package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;

import static server.Utils.between;


public class RequestPredecessorResultOperation implements Operation {

    private NodeInfo predecessor;

    public RequestPredecessorResultOperation(NodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.finishPredecessorRequest(predecessor);
    }
}
