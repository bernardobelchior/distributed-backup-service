package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

public class PingResponseOperation extends Operation {

    public PingResponseOperation(NodeInfo origin) {
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        currentNode.onPingResponse(origin);
    }
}
