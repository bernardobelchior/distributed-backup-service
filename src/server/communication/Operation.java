package server.communication;

import server.dht.DistributedHashTable;

public interface Operation {
    void run(DistributedHashTable<?> dht);
}
