package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.io.IOException;

public class RequestPredecessorOperation extends Operation {

    public RequestPredecessorOperation(NodeInfo origin) {
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        NodeInfo predecessor = currentNode.getPredecessor();
        RequestPredecessorResultOperation operation = new RequestPredecessorResultOperation(origin, predecessor);

        try {
            Mailman.sendOperation(origin, operation);
            currentNode.informAboutExistence(origin);
        } catch (IOException e) {
            System.err.println("Request predecessor informing about failure.");
            currentNode.informAboutFailure(origin);
        }
    }

}
