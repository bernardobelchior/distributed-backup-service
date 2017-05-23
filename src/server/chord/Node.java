package server.chord;

import server.communication.Mailman;
import server.communication.operations.*;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

import static server.chord.FingerTable.LOOKUP_TIMEOUT;
import static server.utils.Utils.addToNodeId;

public class Node {
    public static final int MAX_NODES = 128;
    public static final int REPLICATION_DEGREE = 2;


    private final NodeInfo self;
    private final FingerTable fingerTable;
    private final DistributedHashTable dht;
    private CompletableFuture<NodeInfo> ongoingPredecessorLookup;
    private final ConcurrentHashMap<BigInteger, CompletableFuture<Boolean>> ongoingInsertions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BigInteger, CompletableFuture<Boolean>> ongoingDeletes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BigInteger, CompletableFuture<byte[]>> ongoingGetOperations = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, ConcurrentHashMap<BigInteger, byte[]>> replicatedValues = new ConcurrentHashMap<>();
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

    private CompletableFuture<NodeInfo> requestSuccessorPredecessor(NodeInfo successor) throws IOException, ClassNotFoundException {
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
        if (!fingerTable.findSuccessors(bootstrapperNode))
            return false;

        /*
         * The node now has knowledge of its next r successors,
         * now the finger table will be filled using the successor
         */
        fingerTable.fill();

        NodeInfo successor = fingerTable.getSuccessor();

        /* Get the successor's predecessor, which will be the new node's predecessor */

        CompletableFuture<Void> getPredecessor = null;
        try {
            getPredecessor = requestSuccessorPredecessor(successor).thenAcceptAsync(fingerTable::updatePredecessor, threadPool);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            getPredecessor.get();
        } catch (CancellationException | ExecutionException | InterruptedException e) {
            return false;
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

   
    public boolean store(BigInteger key, byte[] value)  throws ClassNotFoundException{
        if (!dht.backup(key, value))
         return false;
         
        return ensureReplication(key, value);
    }

    private boolean ensureReplication(BigInteger key, byte[] value) {
        NodeInfo node;
        for (int i = 1; i < REPLICATION_DEGREE; i++) {
            try {
                node = fingerTable.getNthSuccessor(i);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Replication of file with key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " failed." +
                        "Current replication degree is " + i + ".");
                return false;
            }

            try {
                Mailman.sendOperation(node, new ReplicationOperation(self, key, value));
            } catch (IOException e) {
                informAboutFailure(node);
                i--;
            }
        }

        return true;
    }

    public void stabilizationProtocol() {
        if (!fingerTable.hasSuccessors())
            return;

        stabilizeSuccessor();
        stabilizePredecessor();
        fingerTable.fill();
    }

    /**
     * Get the node's successor's predecessor, check if it is not the current node
     * and notify the successor of this node's existence
     */
    private void stabilizeSuccessor() {
        pingSuccessor();
        notifySuccessor(getSuccessor());
    }

    private boolean pingSuccessor() {
        BigInteger successorKey = BigInteger.valueOf(addToNodeId(self.getId(), 1));

        CompletableFuture<NodeInfo> findSuccessor = fingerTable.lookup(successorKey);

        try {
            findSuccessor.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException e) {
            fingerTable.onLookupFailed(successorKey);
            return false;
        } catch (InterruptedException | CancellationException e) {
            fingerTable.onLookupFailed(successorKey);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void notifySuccessor(NodeInfo successor) {
        NotifyOperation notification = new NotifyOperation(self);

        try {
            Mailman.sendOperation(successor, notification);
        } catch (IOException e) {
            System.err.println("Unable to notify successor");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void stabilizePredecessor() {
        NodeInfo predecessor = getPredecessor();
        if (self.equals(predecessor) || predecessor == null)
            return;

        BigInteger keyEquivalent = BigInteger.valueOf(predecessor.getId());

        CompletableFuture<Void> predecessorLookup = fingerTable.lookup(keyEquivalent).thenAcceptAsync(
                fingerTable::updatePredecessor,
                threadPool);
        try {
            predecessorLookup.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Predecessor not responding, deleting reference");
            fingerTable.onLookupFailed(keyEquivalent);
            fingerTable.setPredecessor(null);
        }
    }

    public void storeReplica(NodeInfo node, BigInteger key, byte[] value) {
        ConcurrentHashMap<BigInteger, byte[]> replicas = replicatedValues.getOrDefault(node.getId(), new ConcurrentHashMap<>());
        replicas.put(key, value);
        replicatedValues.putIfAbsent(node.getId(), replicas);
    }

    public byte[] getLocalValue(BigInteger key) {
        return dht.getLocalValue(key);
    }

    public DistributedHashTable getDistributedHashTable() {
        return dht;
    }

    CompletableFuture<Boolean> put(BigInteger key, byte[] value) {
        CompletableFuture<Boolean> put = new CompletableFuture<>();

        if (ongoingInsertions.putIfAbsent(key, put) != null) {
            put.completeExceptionally(new Exception("Put operation with same key as " + DatatypeConverter.printHexBinary(key.toByteArray()) + " already ongoing."));
            return put;
        }

        NodeInfo destination;
        try {
            destination = fingerTable.lookup(key).get();
        } catch (InterruptedException | ExecutionException e) {
            fingerTable.onLookupFailed(key);
            System.err.println("Put operation failed. Please try again...");
            e.printStackTrace();
            put.completeExceptionally(e);
            return put;
        }

        try {
            Mailman.sendOperation(destination, new PutOperation(self, key, value));
        } catch (IOException e) {
            e.printStackTrace();
            put.completeExceptionally(e);
        }

        return put;
    }

    public void onPutFinished(BigInteger key, boolean successful) {
        ongoingInsertions.remove(key).complete(successful);
    }

    public boolean updatePredecessor(NodeInfo newPredecessor) {
        if (fingerTable.updatePredecessor(newPredecessor)) {
            dht.getNewPredecessorKeys(newPredecessor);
            return true;
        } else {
            return false;
        }
    }

    public void informAboutExistence(NodeInfo node) {
        fingerTable.updateSuccessors(node);
        fingerTable.updateFingerTable(node);
    }

    public void informAboutFailure(NodeInfo node) {
        System.err.println("Node with ID " + node.getId() + " has failed.");
        fingerTable.informSuccessorsOfFailure(node);
        fingerTable.informFingersOfFailure(node);
        fingerTable.informPredecessorOfFailure(node);
    }

    public boolean hasSuccessors() {
        return fingerTable.hasSuccessors();
    }

    public void onLookupFinished(BigInteger key, NodeInfo targetNode) {
        fingerTable.onLookupFinished(key, targetNode);
        informAboutExistence(targetNode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fingerTable.toString());

        sb.append("\n\nReplicated keys:\n");
        sb.append("NodeID\t\tKey\n");
        replicatedValues.forEach((nodeId, keys) -> keys.forEach((key, value) -> {
            sb.append(nodeId);
            sb.append("\t");
            sb.append(DatatypeConverter.printHexBinary(key.toByteArray()));
            sb.append("\n");
        }));

        return sb.toString();
    }

    public void remappedKeys(ConcurrentHashMap<BigInteger, byte[]> keys) {
        dht.remappedKeys(keys);
    }

    public CompletableFuture<byte[]> get(BigInteger key) {
        CompletableFuture<byte[]> get = new CompletableFuture<>();

        if (ongoingGetOperations.putIfAbsent(key, get) != null) {
            get.completeExceptionally(new Exception("Get operation already ongoing."));
            return get;
        }

        NodeInfo destination;
        try {
            destination = fingerTable.lookup(key).get();
        } catch (InterruptedException | ExecutionException e) {
            fingerTable.onLookupFailed(key);
            System.err.println("Get operation failed. Please try again...");
            e.printStackTrace();
            get.completeExceptionally(e);
            return get;
        }

        try {
            Mailman.sendOperation(destination, new GetOperation(self, key));
        } catch (IOException e) {
            e.printStackTrace();
            get.completeExceptionally(e);
            return get;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return get;
    }

    public void onGetFinished(BigInteger key, byte[] value) {
        ongoingGetOperations.remove(key).complete(value);
    }

    public CompletableFuture<Boolean> remove(BigInteger key) {
        CompletableFuture<Boolean> remove = new CompletableFuture<>();

        if (ongoingDeletes.putIfAbsent(key, remove) != null) {
            remove.completeExceptionally(new Exception("Get operation already ongoing."));
            return remove;
        }

        NodeInfo destination;
        try {
            destination = fingerTable.lookup(key).get();
        } catch (InterruptedException | ExecutionException e) {
            fingerTable.onLookupFailed(key);
            System.err.println("Remove operation failed. Please try again...");
            e.printStackTrace();
            remove.completeExceptionally(e);
            return remove;
        }

        try {
            Mailman.sendOperation(destination, new RemoveOperation(self, key));
        } catch (IOException e) {
            e.printStackTrace();
            remove.completeExceptionally(e);
            return remove;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return remove;
    }


    public void onRemoveFinished(BigInteger key, boolean successful) {
        ongoingDeletes.remove(key).complete(successful);
    }

    public boolean removeValue(BigInteger key) {
        return dht.removeLocally(key);
    }
}
