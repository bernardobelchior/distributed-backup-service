package server.communication;

import server.dht.DistributedHashTable;

import java.math.BigInteger;

public class GetOperation<T> implements Operation {
    private final BigInteger key;
    private final T value;

    GetOperation(BigInteger key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void run(DistributedHashTable<T> dht) {
        dht.completeRetrieval(key, value);
    }
}
