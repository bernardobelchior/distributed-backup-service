package server.communication;

import server.dht.DistributedHashTable;

public class MessageOperation implements Operation {

    @Override
    public void run(DistributedHashTable<?> dht) {
        System.out.println("Ola");
    }
}
