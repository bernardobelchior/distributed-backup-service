package server.communication.operations;
import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

/**
 * Created by epassos on 5/25/17.
 */
public class PingOperation extends Operation{
    public PingOperation(NodeInfo origin){
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        PingResponseOperation response = new PingResponseOperation(currentNode);
        Mailman.sendOperation(origin,response);
    }
}
