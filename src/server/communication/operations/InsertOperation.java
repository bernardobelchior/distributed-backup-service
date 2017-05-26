package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.math.BigInteger;

public class InsertOperation extends Operation {
    private final BigInteger key;
    private final byte[] value;

    public InsertOperation(NodeInfo origin, BigInteger key, byte[] value) {
        super(origin);
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(Node currentNode) {
        InsertResultOperation result = new InsertResultOperation(currentNode.getInfo(), key, currentNode.storeKey(key, value));

        try {
            Mailman.sendOperation(origin, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
