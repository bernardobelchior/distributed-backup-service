package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;

public class GetOperation extends Operation {
    private final BigInteger key;


    public GetOperation(NodeInfo origin, BigInteger key) {
        super(origin);
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        GetResultOperation result = new GetResultOperation(origin, key, currentNode.getLocalValue(key));

        try {
            Mailman.sendOperation(origin, result);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}