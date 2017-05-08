package server.communication;

import server.dht.DistributedHashTable;

public interface Operation<T> {
    void run(DistributedHashTable<T> dht);
}
