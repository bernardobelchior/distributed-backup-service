package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.math.BigInteger;

public class DeleteOperation extends Operation {
    private final BigInteger key;


    public DeleteOperation(NodeInfo origin, BigInteger key) {
        super(origin);
        this.key = key;
    }

    /**
     * This Operation deletes from the given current Node the value with the key.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.removeValue(key);
        DeleteResultOperation result = new DeleteResultOperation(origin, key, currentNode.removeValue(key));

        try {
            Mailman.sendOperation(origin, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
