package server.dht;

import server.chord.Node;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedHashTable<T> {
    private Node self;
    private ConcurrentHashMap<BigInteger, T> hashMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, CompletableFuture<T>> ongoingRetrievals = new ConcurrentHashMap<>();

    public DistributedHashTable(Node self) {
        this.self = self;
    }

    public CompletableFuture<T> get(BigInteger key) throws IOException, ClassNotFoundException {
        return getOrDefault(key, null);
    }

    public CompletableFuture<T> getOrDefault(BigInteger key, T def) throws IOException, ClassNotFoundException {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        ongoingRetrievals.put(key, completableFuture);

        /* If the key is in the current node, return it immediately */
        if (self.inRange(key))
            completableFuture.complete(hashMap.getOrDefault(key, def));

        self.get(key);
        return completableFuture;
    }

    public void completeRetrieval(BigInteger key, T value) {
        ongoingRetrievals.remove(key).complete(value);
    }
}
