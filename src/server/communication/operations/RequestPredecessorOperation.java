package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;

public class RequestPredecessorOperation implements Operation {
    private NodeInfo origin;

    public RequestPredecessorOperation(NodeInfo origin) {
        this.origin = origin;
    }

    @Override
    public void run(Node currentNode) {
        NodeInfo predecessor = currentNode.getFingerTable().getPredecessor();
        currentNode.getFingerTable().updateFingerTable(origin);
        currentNode.getFingerTable().updateSuccessors(origin);
        currentNode.getFingerTable().updatePredecessor(origin);
        RequestPredecessorResultOperation operation = new RequestPredecessorResultOperation(predecessor);
        try {
            Mailman.sendOperation(origin, operation);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
