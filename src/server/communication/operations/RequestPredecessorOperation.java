package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;

/**
 * Created by epassos on 5/15/17.
 */
public class RequestPredecessorOperation implements Operation {
    private NodeInfo origin;

    public RequestPredecessorOperation(NodeInfo origin) {
        this.origin = origin;
    }

    @Override
    public void run(Node currentNode) {
        NodeInfo predecessor = currentNode.getFingerTable().getPredecessor();
        currentNode.getFingerTable().updateFingerTable(origin);
        currentNode.getFingerTable().updatePredecessor(origin);
        RequestPredecessorResultOperation operation = new RequestPredecessorResultOperation(predecessor);
        try {
            Mailman.sendObject(origin, operation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKey() {
        return null;
    }
}
