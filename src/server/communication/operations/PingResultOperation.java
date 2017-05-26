package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.math.BigInteger;

public class PingResultOperation extends Operation {

    public PingResultOperation(NodeInfo origin) {
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Received Pong, removing key " + origin.getId());
        Mailman.ongoingPings.operationFinished(origin.getId(), null);
    }
}
