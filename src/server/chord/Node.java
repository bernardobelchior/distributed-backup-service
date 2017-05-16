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
    private final ScheduledThreadPoolExecutor stabilizationExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

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
        return lookupFrom(key, bestNextNode);
    }

    /**
     * Search for a key, starting from a specific node
     * @param key
     * @param nodeToLookup
     * @return
     * @throws IOException
     */
    private CompletableFuture<NodeInfo> lookupFrom(BigInteger key, NodeInfo nodeToLookup) throws IOException {

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

        /* Get the node's successor, simple lookup on the DHT */

        BigInteger successorKey = BigInteger.valueOf(Integer.remainderUnsigned(self.getId() + 1, MAX_NODES));

        CompletableFuture<Void> successorLookup = lookupFrom(successorKey, bootstrapperNode).thenAcceptAsync(
                successor -> fingerTable.setFinger(0, successor), threadPool);

        successorLookup.get();

        boolean completedOK = !successorLookup.isCompletedExceptionally() && !successorLookup.isCancelled();

        if (!completedOK)
            return false;

        /*
         * The node now has knowledge of its successor,
         * now the finger table will be filled using the successor
         */

        fillFingerTable();

        NodeInfo successor = fingerTable.getSuccessor();

        /* Get the successor's predecessor, which will be the new node's predecessor */

        CompletableFuture<Void> getPredecessor = requestPredecessor(successor).thenAcceptAsync(fingerTable::setPredecessor, threadPool);
        getPredecessor.get();
        completedOK = !getPredecessor.isCompletedExceptionally() && !getPredecessor.isCancelled();

        return completedOK;
    }

    public void setDHT(DistributedHashTable<?> dht) {
        this.dht = dht;
    }

    private void fillFingerTable() throws Exception {
        fingerTable.fill(this);
    }

    /**
     * Search the finger table for the next best node
     * @param key key that is being searched
     * @return NodeInfo for the closest preceding node to the searched key
     */
    public NodeInfo getNextBestNode(BigInteger key) {
        return fingerTable.getNextBestNode(key);
    }


    /**
     * Get the node's successor (finger table's first entry)
     * @return NodeInfo for the node's successor
     */
    public NodeInfo getSuccessor() {
        return fingerTable.getSuccessor();
    }



    /**
     * Called by a LookupResultOperation, signals the lookup for the key is finished
     * @param key key that was searched
     * @param targetNode node responsible for the key
     */
    public void finishedLookup(BigInteger key, NodeInfo targetNode) {
        CompletableFuture<NodeInfo> result = ongoingLookups.remove(key);
        result.complete(targetNode);
    }

    public void finishPredecessorRequest(NodeInfo predecessor) {
        fingerTable.setPredecessor(predecessor);
        predecessorLookup.complete(predecessor);
    }

    public void initializeStabilization() {
        stabilizationExecutor.scheduleWithFixedDelay(() -> {
            stabilizationProtocol();
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void stabilizationProtocol() {

    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
