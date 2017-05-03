package server.communication;

import server.dht.DistributedHashTable;

public class MessageOperation<T> implements Operation<T> {

    @Override
    public void run(DistributedHashTable<T> dht) {
        System.out.println("Ola");
    }
}
