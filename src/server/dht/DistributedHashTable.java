package server.dht;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.operations.LookupOperation;

import java.io.IOException;
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

    /**
     * Starts the process of joining an already established network
     *
     * @param bootstrapper Node to lookup information from.
     */
    public void bootstrapNode(NodeInfo bootstrapper) throws IOException {
        new Thread(() -> {
            try {
                Mailman.sendObject(
                        bootstrapper,
                        new LookupOperation(
                                self.getInfo(),
                                BigInteger.valueOf(self.getInfo().getId())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void lookup(LookupOperation lookupOperation) {
        /* FIXME:
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
        }*/
    }

    public String getState() {
        return self.getFingerTable().toString();
    }
}
