package server.dht;

import server.chord.Node;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedHashTable<T> {
    private Node self;
    private ConcurrentHashMap<BigInteger, T> localValues = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, CompletableFuture<T>> ongoingRetrievals = new ConcurrentHashMap<>();

    public DistributedHashTable(Node self) {
        this.self = self;
    }

    public T get(BigInteger key) {
        return getOrDefault(key, null);
    }

    public T getOrDefault(BigInteger key, T def) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        ongoingRetrievals.put(key, completableFuture);

        /* If the key is in the current node, return it immediately */
        /*
        FIXME:
        if (self.inRange(key))
            completableFuture.complete(localValues.getOrDefault(key, def));

        try {
            self.lookup(key);
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
            return def;
        }*/
        return null;
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Node ID: ");
        sb.append(self.getInfo().getId());
        sb.append("\n\n");

        sb.append(self.getFingerTable().toString());
        return sb.toString();
    }
}
