package server.chord;

import server.communication.Mailman;
import server.communication.operations.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

import static server.chord.FingerTable.NUM_SUCCESSORS;
import static server.utils.Utils.addToNodeId;
import static server.utils.Utils.between;

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
    private CompletableFuture<NodeInfo> lookupFrom(BigInteger key, NodeInfo nodeToLookup) {
        /* Check if requested lookup is already being done */
        CompletableFuture<NodeInfo> lookupResult = ongoingLookups.get(key);

        if (lookupResult != null)
            return lookupResult;

        lookupResult = new CompletableFuture<>();
        ongoingLookups.put(key, lookupResult);

        try {
            Mailman.sendOperation(nodeToLookup, new LookupOperation(self, key));
        } catch (IOException e) {
            lookupResult.completeExceptionally(e);
        }

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
    public boolean bootstrap(NodeInfo bootstrapperNode) {

        /* Get the node's successors */
        if (!findSuccessors(bootstrapperNode))
            return false;

        /*
         * The node now has knowledge of its next r successors,
         * now the finger table will be filled using the successor
         */
        fingerTable.fill(this);

        NodeInfo successor = fingerTable.getSuccessor();

        /* Get the successor's predecessor, which will be the new node's predecessor */

        CompletableFuture<Void> getPredecessor;
        try {
            getPredecessor = requestSucessorPredecessor(successor).thenAcceptAsync(fingerTable::updatePredecessor, threadPool);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            getPredecessor.get();
        } catch (CancellationException | ExecutionException | InterruptedException e) {
            return false;
        }

        return true;
    }

    private boolean findSuccessors(NodeInfo bootstrapperNode) {
        BigInteger successorKey = BigInteger.valueOf(addToNodeId(self.getId(), 1));

        for (int i = 0; i < NUM_SUCCESSORS; i++) {
            CompletableFuture<NodeInfo> successorLookup = lookupFrom(successorKey, bootstrapperNode);

            try {
                successorLookup.get();
            } catch (InterruptedException | ExecutionException e) {
                /* If the lookup did not complete correctly */
                return false;
            }

            try {
                successorKey = BigInteger.valueOf(addToNodeId(fingerTable.getNthSuccessor(i).getId(), 1));
            } catch (IndexOutOfBoundsException e) {
                /* This means that there is no Nth successor. As such, we treat it as a normal thing that only
                 * happens when the network has a number of nodes lower than NUM_SUCCESSORS. */
                break;
            }
        }

        return true;
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
        fingerTable.informAbout(targetNode);
    }

    public void finishPredecessorRequest(NodeInfo predecessor) {
        fingerTable.updatePredecessor(predecessor);

        if (ongoingPredecessorLookup == null)
            System.out.println("Null");
        ongoingPredecessorLookup.complete(predecessor);
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
        if (!fingerTable.hasSuccessors())
            return;

        System.out.println("Started Stabilization");
        try {
            stabilizeSuccessor();
        } catch (Exception e) {
            // treat
            e.printStackTrace();
        }
        try {
            stabilizePredecessor();
        } catch (Exception e) {
            //treat
            e.printStackTrace();
        }
        try {
            System.out.println("Exiting");
            fingerTable.fill(this);
            System.out.println("Exiting");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Ended Stabilization");
    }

    /**
     * Get the node's successor's predecessor, check if it is not the current node
     * and notify the successor of this node's existence
     */
    private void stabilizeSuccessor() {
        NodeInfo successor = getSuccessor();

        /* Can happen if no other node has connected yet */
        if (self.equals(successor))
            return;

        try {
            checkSuccessorStatus();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            fingerTable.deleteSuccessor();
            try {
                fingerTable.fill(this);
            } catch (Exception ex) {
                System.err.println("Unable to fill finger table");
                return;
            }
        }

        successor = getSuccessor();

        NodeInfo finalSuccessor = successor;

        try {
            CompletableFuture<Void> getSuccessorPredecessor = requestSucessorPredecessor(successor).thenAcceptAsync(
                    successorPredecessor -> {

                        if (self.equals(successorPredecessor))
                            return;

                        if (successorPredecessor == null)
                            return;

                    /*
                     * Check if the predecessor of my successor should be my successor
                     * i.e. if successorPredecessor is in the interval [node,successor]
                     */
                        if (between(self.getId(), finalSuccessor.getId(), successorPredecessor.getId()))
                            fingerTable.setFinger(0, successorPredecessor);

                    },
                    threadPool);

            getSuccessorPredecessor.get(400, TimeUnit.MILLISECONDS);

            /* Tell my new successor I should be his predecessor */
            notifySuccessor(getSuccessor());

        } catch (InterruptedException | ExecutionException | IOException | TimeoutException e) {
            return;
        }
    }

    private void checkSuccessorStatus() throws IOException {
        BigInteger successorKey = BigInteger.valueOf(addToNodeId(self.getId(), 1));

        CompletableFuture<Void> findSuccessor = lookup(successorKey).thenAcceptAsync(
                successor -> {
                    NodeInfo currentSuccessor = getSuccessor();
                    if (getSuccessor() == null || between(self.getId(), currentSuccessor.getId(), successor.getId()))
                        fingerTable.setSuccessor(successor, 0);
                }
        );

        try {
            findSuccessor.get(400, TimeUnit.MILLISECONDS);


        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new IOException("Successor not responding, deleting reference");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (findSuccessor.isCompletedExceptionally() || findSuccessor.isCancelled())
            throw new IOException("Successor not responding, deleting reference");
    }

    private void notifySuccessor(NodeInfo successor) {
        NotifyOperation notification = new NotifyOperation(self);
        try {
            Mailman.sendOperation(successor, notification);
            System.out.println("Notification sent");
        } catch (IOException e) {
            System.err.println("Unable to notify successor");
            return;
        }
    }

    private void stabilizePredecessor() {
        NodeInfo predecessor = getPredecessor();
        if (self.equals(predecessor) || predecessor == null)
            return;

        BigInteger keyEquivalent = BigInteger.valueOf(predecessor.getId());

        try {
            CompletableFuture<Void> predecessorLookup = lookup(keyEquivalent).thenAcceptAsync(
                    fingerTable::updatePredecessor,
                    threadPool);


            predecessorLookup.get(400, TimeUnit.MILLISECONDS);

            if (predecessorLookup.isCompletedExceptionally() || predecessorLookup.isCancelled()) {
                System.err.println("Predecessor not responding, deleting reference");
                fingerTable.setPredecessor(null);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            System.err.println("Predecessor not responding, deleting reference");
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

    public void informAbout(NodeInfo node) {
        fingerTable.informAbout(node);
    }

    public boolean hasSuccessors() {
        return fingerTable.hasSuccessors();
    }
}
