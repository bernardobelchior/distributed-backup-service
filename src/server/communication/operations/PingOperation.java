package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

public class PingOperation extends Operation {
    public PingOperation(NodeInfo origin) {
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        try {
            Mailman.sendPong(origin);
        } catch (Exception e) {
            System.err.println("Error on Ping Operation");
        }
    }
}
