package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.io.IOException;
import java.math.BigInteger;

public class DeleteOperation extends Operation {
    private final BigInteger key;


    public DeleteOperation(NodeInfo origin, BigInteger key) {
        super(origin);
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.removeValue(key);
        DeleteResultOperation result = new DeleteResultOperation(origin, key, currentNode.removeValue(key));

        try {
            Mailman.sendOperation(origin, result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
