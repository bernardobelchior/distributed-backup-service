package server.dht;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.LookupOperation;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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
        if (self.inRange(key))
            completableFuture.complete(localValues.getOrDefault(key, def));

        try {
            self.lookup(key);
            return completableFuture.get();
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
            return def;
        }
    }

    public void completeRetrieval(BigInteger key, T value) {
        ongoingRetrievals.remove(key).complete(value);
    }

    public boolean inRangeOfCurrentNode(BigInteger key) {
        return self.inRange(key);
    }

    /**
     * Starts the process of joining an already established network
     *
     * @param node         Node to bootstrap.
     * @param bootstrapper Node to lookup information from.
     */
    public void bootstrapNode(Node node, NodeInfo bootstrapper) throws IOException {
        Mailman.sendObject(
                bootstrapper,
                new LookupOperation<T>(
                        self.getInfo(),
                        BigInteger.valueOf(self.getInfo().getId())));
    }

    public void lookup(LookupOperation<T> lookupOperation) {
        if (inRangeOfCurrentNode(lookupOperation.getKey())) {
            try {
                Mailman.sendObject(lookupOperation.getOrigin(), localValues.get(lookupOperation.getKey()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                self.lookup(lookupOperation.getKey(), lookupOperation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
