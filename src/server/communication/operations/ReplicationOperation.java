package server.communication.operations;

import server.chord.Node;

import java.math.BigInteger;

public class ReplicationOperation<T> implements Operation<T>{
    private final BigInteger key;
    private final T value;

    public ReplicationOperation(BigInteger key, T value)  {
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(Node<T> currentNode) {
        currentNode.backup(key, value);
    }
}
