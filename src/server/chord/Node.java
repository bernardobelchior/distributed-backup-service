package server.chord;

import server.communication.Mailman;
import server.communication.operations.LookupOperation;
import server.communication.operations.NotifyOperation;
import server.communication.operations.PutOperation;
import server.communication.operations.ReplicationOperation;
import server.communication.operations.RequestPredecessorOperation;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

import static server.Utils.between;

public class Node {
    public static final int MAX_NODES = 128;
    public static final int REPLICATION_DEGREE = 2;

    private final NodeInfo self;
    private final FingerTable fingerTable;
    private final DistributedHashTable dht;
    private final ConcurrentHashMap<BigInteger, CompletableFuture<NodeInfo>> ongoingLookups = new ConcurrentHashMap<>();
    private CompletableFuture<NodeInfo> ongoingPredecessorLookup;
    private final ConcurrentHashMap<BigInteger, CompletableFuture<Boolean>> ongoingInsertions = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService stabilizationExecutor = Executors.newScheduledThreadPool(5);
    private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(5);

    /**
     * @param port Port to start the service in
     */
    public Node(int port) throws IOException, NoSuchAlgorithmException {
        self = new NodeInfo(InetAddress.getLocalHost(), port);
        fingerTable = new FingerTable(self);
        ongoingPredecessorLookup = null;
        dht = new DistributedHashTable(this);
    }

    public CompletableFuture<NodeInfo> lookup(BigInteger key) throws IOException {
        if (fingerTable.keyBelongsToSuccessor(key))
            return lookupFrom(key, fingerTable.getSuccessor());
        else
            return lookupFrom(key, fingerTable.getNextBestNode(key));
    }

    /**
     * Search for a key, starting from a specific node
     *
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

        Mailman.sendOperation(nodeToLookup, new LookupOperation(self, key));

        return lookupResult;
    }

    private CompletableFuture<NodeInfo> requestSucessorPredecessor(NodeInfo successor) throws IOException {

        /* Check if the request is already being made */

        CompletableFuture<NodeInfo> requestResult = ongoingPredecessorLookup;
        if (requestResult != null)
            return requestResult;

        requestResult = new CompletableFuture<>();
        ongoingPredecessorLookup = requestResult;
        Mailman.sendOperation(successor, new RequestPredecessorOperation(self));

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

        /* Get the node's successors */


        if (!findSuccessors(bootstrapperNode))
            return false;

        /*
         * The node now has knowledge of its next r successors,
         * now the finger table will be filled using the successor
         */

        fillFingerTable();

        NodeInfo successor = fingerTable.getSuccessor();

        /* Get the successor's predecessor, which will be the new node's predecessor */

        CompletableFuture<Void> getPredecessor = requestSucessorPredecessor(successor).thenAcceptAsync(fingerTable::updatePredecessor, threadPool);
        getPredecessor.get();
        return !getPredecessor.isCompletedExceptionally() && !getPredecessor.isCancelled();
    }

    private boolean findSuccessors(NodeInfo bootstrapperNode) throws IOException, ExecutionException, InterruptedException {

        BigInteger successorKey = BigInteger.valueOf(Integer.remainderUnsigned(self.getId() + 1, MAX_NODES));

        for (int i = 0; i < FingerTable.NUM_SUCCESSORS; i++) {

            int index = i;
            System.out.format("Finding successor %d, key is %d",i, successorKey);
            CompletableFuture<Void> successorLookup = lookupFrom(successorKey, bootstrapperNode).thenAcceptAsync(
                    successor -> {
                        NodeInfo firstSuccessor = getSuccessor();
                        if ((firstSuccessor == null || !successor.equals(firstSuccessor)) && !successor.equals(self))
                            fingerTable.setSuccessor(successor, index);
                    },
                    threadPool);

            successorLookup.get();

            boolean completedOK = !successorLookup.isCompletedExceptionally() && !successorLookup.isCancelled();

            if (!completedOK)
                return false;

            if (fingerTable.getNthSuccessor(i) == null)
                break;

            successorKey = BigInteger.valueOf(Integer.remainderUnsigned(fingerTable.getNthSuccessor(i).getId() + 1, MAX_NODES));
        }

        return true;
    }

    private void fillFingerTable() throws Exception {
        fingerTable.fill(this);
    }

    /**
     * Search the finger table for the next best node
     *
     * @param key key that is being searched
     * @return NodeInfo for the closest preceding node to the searched key
     */
    public NodeInfo getNextBestNode(BigInteger key) {
        return fingerTable.getNextBestNode(key);
    }

    /**
     * Get the node's successor (finger table's first entry)
     *
     * @return NodeInfo for the node's successor
     */
    public NodeInfo getSuccessor() {
        return fingerTable.getSuccessor();
    }

    /**
     * Get the node's predecessor
     *
     * @return NodeInfo for the node's predecessor
     */
    public NodeInfo getPredecessor() {
        return fingerTable.getPredecessor();
    }

    /**
     * Called by a LookupResultOperation, signals the lookup for the key is finished
     *
     * @param key        key that was searched
     * @param targetNode node responsible for the key
     */
    public void finishedLookup(BigInteger key, NodeInfo targetNode) {
        CompletableFuture<NodeInfo> result = ongoingLookups.remove(key);
        result.complete(targetNode);
    }

    public void finishPredecessorRequest(NodeInfo predecessor) {
        System.out.println("A");
        fingerTable.updatePredecessor(predecessor);
        System.out.println("B");

        if (ongoingPredecessorLookup == null)
            System.out.println("Null");
        System.out.println("C");
        ongoingPredecessorLookup.complete(predecessor);
        System.out.println("D");
        ongoingPredecessorLookup = null;
    }

    public void initializeStabilization() {
        stabilizationExecutor.scheduleWithFixedDelay(this::stabilizationProtocol, 5, 5, TimeUnit.SECONDS);

    }


    public boolean store(BigInteger key, byte[] value) {
        if (!dht.storeLocally(key, value))
            return false;

        //FIXME: Use REPLICATION_DEGREE when list of R successors is done.
        NodeInfo successor = fingerTable.getSuccessor();

        if (successor.equals(self)) {
            //FIXME:
            System.err.println("I got no successor. What do I do?");
        } else {
            try {
                Mailman.sendOperation(successor, new ReplicationOperation(key, value));
            } catch (IOException e) {
                System.err.println("Could not start the replication operation.");
                e.printStackTrace();
            }
        }

        return true;
    }

    public void stabilizationProtocol() {
        System.out.println("Started Stabilization");
        try {
            System.out.println("Stabilizing successor");
            stabilizeSuccessor();
            System.out.println("Stabilized successor");
        } catch (Exception e) {
            // treat
            e.printStackTrace();
        }
        try {
            System.out.println("Stabilizing predecessor");
            stabilizePredecessor();
            System.out.println("Stabilized predecessor");
        } catch (Exception e) {
            //treat
            e.printStackTrace();
        }
        try {
            fillFingerTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Ended Stabilization");
    }

    /**
     * Get the node's successor's predecessor, check if it is not the current node
     * and notify the successor of this node's existence
     *
     * @throws Exception
     */
    private void stabilizeSuccessor() throws Exception {
        NodeInfo successor = getSuccessor();

        /* Can happen if no other node has connected yet */
        if (successor.equals(self))
            return;

        checkSuccessorStatus();

        successor = getSuccessor();

        NodeInfo finalSuccessor = successor;

        CompletableFuture<Void> getSuccessorPredecessor = requestSucessorPredecessor(successor).thenAcceptAsync(
                successorPredecessor -> {

                    if(self.equals(successorPredecessor))
                        return;

                    /*
                     * Check if the predecessor of my successor should be my successor
                     * i.e. if successorPredecessor is in the interval [node,successor]
                     */
                    if (between(self.getId(), finalSuccessor.getId(), successorPredecessor.getId()))
                        fingerTable.setFinger(0, successorPredecessor);

                    /* Tell my new successor I should be his predecessor */
                    notifySuccessor(successorPredecessor);
                },
                threadPool);

        System.out.println("2");
        getSuccessorPredecessor.get();
        System.out.println("3");
    }

    private void checkSuccessorStatus() throws Exception {
        BigInteger successorKey = BigInteger.valueOf(Integer.remainderUnsigned(self.getId() + 1,MAX_NODES));
        CompletableFuture<NodeInfo> findSuccessor = lookup(successorKey);
        CompletableFuture timeoutFuture = new CompletableFuture();

        timeoutExecutor.schedule(
                () -> timeoutFuture.completeExceptionally(new TimeoutException()),
                400, TimeUnit.MILLISECONDS);

        CompletableFuture result = CompletableFuture.anyOf(findSuccessor,timeoutFuture);

        if (result.isCompletedExceptionally() || result.isCancelled()) {
            System.out.println("Successor not responding, deleting reference");
            fingerTable.deleteSuccessor();
            fillFingerTable();
        }
    }

    private void notifySuccessor(NodeInfo successor) {
        NotifyOperation notification = new NotifyOperation(self);
        try {
            Mailman.sendOperation(successor, notification);
            System.out.println("Notification sent");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stabilizePredecessor() throws Exception {
        NodeInfo predecessor = getPredecessor();
        if (predecessor.equals(self))
            return;

        BigInteger keyEquivalent = BigInteger.valueOf(predecessor.getId());

        CompletableFuture<Void> predecessorLookup = lookup(keyEquivalent).thenAcceptAsync(
                fingerTable::updatePredecessor,
                threadPool);

        CompletableFuture timeoutFuture = new CompletableFuture();

        timeoutExecutor.schedule(
                () -> timeoutFuture.completeExceptionally(new TimeoutException()),
                400, TimeUnit.MILLISECONDS);

        CompletableFuture result = CompletableFuture.anyOf(predecessorLookup, timeoutFuture);

        if (result.isCompletedExceptionally() || result.isCancelled()) {
            System.out.println("Predecessor not responding, deleting reference");
            fingerTable.setPredecessor(null);
        }
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void backup(BigInteger key, byte[] value) {
        dht.backup(key, value);
    }

    public DistributedHashTable getDistributedHashTable() {
        return dht;
    }

    public CompletableFuture<Boolean> put(BigInteger key, byte[] value) {
        CompletableFuture<Boolean> put = new CompletableFuture<>();

        if (ongoingInsertions.putIfAbsent(key, put) != null) {
            put.completeExceptionally(new Exception("Put operation already ongoing."));
            return put;
        }

        try {
            NodeInfo destination = lookup(key).get();
            PutOperation putOperation = new PutOperation(self, key, value);
            Mailman.sendOperation(destination, putOperation);
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.err.println("Put operation failed. Please try again...");
            e.printStackTrace();
            put.completeExceptionally(e);
        }

        return put;
    }

    public void finishedPut(BigInteger key, boolean successful) {
        ongoingInsertions.remove(key).complete(successful);
    }
}
