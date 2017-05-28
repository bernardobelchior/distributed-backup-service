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

    /**
     * This Operation finishes the Request Predecessor Operation and removes it from the operation manager.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.finishPredecessorRequest(predecessor);
    }
}
