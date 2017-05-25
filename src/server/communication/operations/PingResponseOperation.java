package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

/**
 * Created by epassos on 5/25/17.
 */
public class PingResponseOperation extends Operation {

    public PingResponseOperation(NodeInfo origin){
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Received ping response from node with ID " + origin.getId());
        currentNode.onPingResponse(origin);
    }
}
