package server.communication.operations;
import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.io.IOException;

public class PingOperation extends Operation{
    public PingOperation(NodeInfo origin){
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        PingResponseOperation response = new PingResponseOperation(currentNode.getInfo());
        try {
            Mailman.sendOperation(origin,response);
        } catch (IOException e) {
            System.err.println("Error on Ping Operation");
        }
    }
}
