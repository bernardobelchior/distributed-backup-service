package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;

public class PutOperation<T> implements Operation<T> {
    private final BigInteger key;
    private final T value;
    private final NodeInfo origin;

    public PutOperation(NodeInfo origin, BigInteger key, T value) {
        this.origin = origin;
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(Node<T> currentNode) {
        PutResultOperation<T> result = new PutResultOperation<>(origin, key, currentNode.store(key, value));

        if (currentNode.getInfo().equals(origin)) {
            result.run(currentNode);
        } else {
            try {
                Mailman.sendObject(origin, result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
