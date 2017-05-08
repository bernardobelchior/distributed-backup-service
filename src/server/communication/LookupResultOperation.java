package server.communication;

import server.chord.NodeInfo;
import server.dht.DistributedHashTable;

import java.math.BigInteger;

public class LookupResultOperation<T> implements Operation<T> {
    private BigInteger key;
    private T value;
    private NodeInfo origin;

    public LookupResultOperation(NodeInfo origin, BigInteger key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(DistributedHashTable<T> dht) {
        dht.completeRetrieval(key, value);
    }
}
