package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;


public class RequestPredecessorResultOperation extends Operation {

    private NodeInfo predecessor;

    public RequestPredecessorResultOperation(NodeInfo origin, NodeInfo predecessor) {
        super(origin);
        this.predecessor = predecessor;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.finishPredecessorRequest(predecessor);
    }
}
