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
        currentNode.finishPredecessorRequest(predecessor);
    }

    @Override
    public String getKey() {
        return null;
    }
}
