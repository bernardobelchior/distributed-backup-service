package server.dht;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.LookupOperation;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

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

    public boolean inRangeOfCurrentNode(BigInteger key) {
        return self.inRange(key);
    }

    /**
     * Starts the process of joining an already established network
     *
     * @param bootstrapper server.chord.Node to get information from.
     */
    public void bootstrapNode(NodeInfo bootstrapper) throws IOException {
        SSLSocket sslSocket = (SSLSocket)
                SSLSocketFactory.getDefault().createSocket
                        (bootstrapper.getAddress(), bootstrapper.getPort());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());

        objectOutputStream.writeObject(
                new LookupOperation<T>(
                        self.getInfo(),
                        BigInteger.valueOf(self.getInfo().getId())));
    }
}
