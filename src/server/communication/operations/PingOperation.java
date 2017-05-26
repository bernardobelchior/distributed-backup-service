package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

public class PingOperation extends Operation {
    boolean isPong;

    public PingOperation(NodeInfo origin) {
        super(origin);
        this.isPong = isPong;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Ping received");
        try {
            Mailman.sendPong(origin);
        } catch (Exception e) {
            System.err.println("Error on Ping Operation");
        }
    }
}
