package server.chord;

import server.communication.Mailman;
import server.communication.operations.LookupOperation;
import server.communication.operations.RequestPredecessorOperation;
import server.dht.DistributedHashTable;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

public class Node {
    public static final int MAX_NODES = 128;

    private final NodeInfo self;
    private final FingerTable fingerTable;
    private DistributedHashTable<?> dht;
    private final ConcurrentHashMap<BigInteger, CompletableFuture<NodeInfo>> ongoingLookups = new ConcurrentHashMap<>();
    private CompletableFuture<NodeInfo> predecessorLookup;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws IOException, NoSuchAlgorithmException {
        self = new NodeInfo(InetAddress.getLocalHost(), port);
        fingerTable = new FingerTable(self);
        predecessorLookup = null;
    }

    public CompletableFuture<NodeInfo> lookup(BigInteger key) throws IOException {
        NodeInfo bestNextNode = fingerTable.getNextBestNode(key);
        return lookup(key, bestNextNode);
    }

    private CompletableFuture<NodeInfo> lookup(BigInteger key, NodeInfo nodeToLookup) throws IOException {

        /* Check if requested lookup is already being done */
        CompletableFuture<NodeInfo> lookupResult = ongoingLookups.get(key);

        if (lookupResult != null)
            return lookupResult;

        lookupResult = new CompletableFuture<>();
        ongoingLookups.put(key, lookupResult);

        Mailman.sendObject(nodeToLookup, new LookupOperation(self, key));

        return lookupResult;
    }

    private CompletableFuture<NodeInfo> requestPredecessor(NodeInfo successor) throws IOException {

        /* Check if the request is already being made */
        CompletableFuture<NodeInfo> requestResult = predecessorLookup;
        if (requestResult != null)
            return requestResult;

        requestResult = new CompletableFuture<>();
        predecessorLookup = requestResult;
        Mailman.sendObject(successor, new RequestPredecessorOperation(self));

        return requestResult;
    }

    public NodeInfo getInfo() {
        return self;
    }

    public FingerTable getFingerTable() {
        return fingerTable;
    }

    public boolean keyBelongsToSuccessor(BigInteger key) {
        return fingerTable.keyBelongsToSuccessor(key);
    }

    /**
     * Add the node to the network and update its finger table.
     *
     * @param bootstrapperNode Node that will provide the information with which our finger table will be updated.
     */
    public boolean bootstrap(NodeInfo bootstrapperNode) throws Exception {
        BigInteger successorKey = BigInteger.valueOf(Integer.remainderUnsigned(self.getId() + 1, MAX_NODES));

        CompletableFuture<Void> successorLookup = lookup(successorKey, bootstrapperNode).thenAcceptAsync(
                successor -> fingerTable.updateSuccessor(0, successor));

        successorLookup.get();


        boolean completedOK = !successorLookup.isCompletedExceptionally() && !successorLookup.isCancelled();

        if (!completedOK)
            return false;

        fillFingerTable();

        NodeInfo successor = fingerTable.getSuccessor();
        CompletableFuture<Void> getPredecessor = requestPredecessor(successor).thenAcceptAsync(fingerTable::setPredecessor);

        System.out.println("fetching predecessor");
        getPredecessor.get();
        System.out.println("fetched predecessor");

        completedOK = !getPredecessor.isCompletedExceptionally() && !getPredecessor.isCancelled();

        return completedOK;
    }


    public void finishedLookup(BigInteger key, NodeInfo targetNode) {
        CompletableFuture<NodeInfo> result = ongoingLookups.remove(key);
        result.complete(targetNode);
    }

    public void setDHT(DistributedHashTable<?> dht) {
        this.dht = dht;
    }

    private void fillFingerTable() throws Exception {
        fingerTable.fill(this);
    }

    public NodeInfo getNextBestNode(BigInteger key) {
        return fingerTable.getNextBestNode(key);
    }


    public NodeInfo getSuccessor() {
        return fingerTable.getSuccessor();
    }

    public boolean lookupSuccessor(int index, BigInteger keyToLookup) throws IOException, ExecutionException, InterruptedException {
        CompletableFuture<Void> successorLookup = lookup(keyToLookup).thenAcceptAsync(
                successor -> fingerTable.updateSuccessor(index, successor), threadPool);

        successorLookup.get();

        return !successorLookup.isCompletedExceptionally() && !successorLookup.isCancelled();
    }

    public void finishPredecessorRequest(NodeInfo predecessor) {
        fingerTable.setPredecessor(predecessor);
        System.out.println("Exiting");
        if(predecessorLookup == null)
            System.out.println("Null");
        predecessorLookup.complete(predecessor);
    }
}
