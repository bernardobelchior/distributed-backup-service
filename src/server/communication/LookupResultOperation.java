package server.communication;

import server.chord.NodeInfo;
import server.dht.DistributedHashTable;

import java.math.BigInteger;

public class LookupResultOperation<T> implements Operation<T> {
    private BigInteger key;
    private T value;
    private NodeInfo origin;

    public LookupResultOperation(NodeInfo origin, BigInteger key, T value) {
        this.origin = origin;
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(DistributedHashTable<T> dht) {
        dht.completedLookup(this);
    }

    public BigInteger getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public NodeInfo getOrigin() {
        return origin;
    }
}
