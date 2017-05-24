package server.chord;

import server.communication.Mailman;
import server.communication.operations.*;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

import static server.chord.DistributedHashTable.OPERATION_TIMEOUT;
import static server.chord.FingerTable.LOOKUP_TIMEOUT;
import static server.utils.Utils.addToNodeId;

public class Node {
    public static final int MAX_NODES = 128;
    private static final int REPLICATION_DEGREE = 2;


    private final NodeInfo self;
    private final FingerTable fingerTable;
    private final DistributedHashTable dht;
    private CompletableFuture<NodeInfo> ongoingPredecessorLookup;

    public final OperationManager<Integer, Boolean> ongoingKeySendings = new OperationManager<>();

    public final OperationManager<BigInteger, Boolean> ongoingDeletes = new OperationManager<>();
    public final OperationManager<BigInteger, Boolean> ongoingPuts = new OperationManager<>();
    public final OperationManager<BigInteger, byte[]> ongoingGets = new OperationManager<>();

    private final ConcurrentHashMap<Integer, ConcurrentHashMap<BigInteger, byte[]>> replicatedValues = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService stabilizationExecutor = Executors.newScheduledThreadPool(5);

    /**
     * @param address Address of this server
     * @param port    Port to start the service in
     */
    public Node(InetAddress address, int port) throws IOException, NoSuchAlgorithmException {
        self = new NodeInfo(address, port);
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

    public boolean store(BigInteger key, byte[] value) {
        return dht.backup(key, value) && ensureReplication(key, value);
    }

    private boolean ensureReplication(BigInteger key, byte[] value) {
        NodeInfo node;
        for (int i = 1; i < REPLICATION_DEGREE; i++) {
            try {
                node = fingerTable.getNthSuccessor(i);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Replication of file with key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " failed.\n" +
                        "Current replication degree is " + i + ".");
                //FIXME: Try again later.
                return true;
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
            e.printStackTrace();
            System.err.println("Predecessor not responding, deleting reference");
            Mailman.state();
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
        return operation(ongoingPuts, new PutOperation(self, key, value), key);
    }

    private CompletableFuture<Boolean> sendKeysToNode(NodeInfo destination, ConcurrentHashMap<BigInteger, byte[]> keys) throws IOException {
        int destinationId = destination.getId();
        CompletableFuture<Boolean> sending = ongoingKeySendings.putIfAbsent(destinationId);

        if (sending != null)
            return sending;

        Mailman.sendOperation(destination, new SendKeysOperation(self, keys));

        return ongoingKeySendings.get(destinationId);
    }

    public boolean updatePredecessor(NodeInfo newPredecessor) {
        if (fingerTable.updatePredecessor(newPredecessor)) {

            try {
                sendKeysToNode(newPredecessor,
                        dht.getNewPredecessorKeys(newPredecessor)).get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
                informAboutFailure(newPredecessor);
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public void informAboutExistence(NodeInfo node) {
        fingerTable.updatePredecessor(node);
        fingerTable.updateSuccessors(node);
        fingerTable.updateFingerTable(node);
    }

    public void informAboutFailure(NodeInfo node) {
        System.err.println("Node with ID " + node.getId() + " has failed.");
        fingerTable.informSuccessorsOfFailure(node);
        fingerTable.informFingersOfFailure(node);
        fingerTable.informPredecessorOfFailure(node);
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

    public void receivedSuccessorKeys(ConcurrentHashMap<BigInteger, byte[]> keys) {
        dht.storePredecessorKeys(keys);
    }

    CompletableFuture<byte[]> get(BigInteger key) {
        return operation(ongoingGets, new GetOperation(self, key), key);
    }

    private <R> CompletableFuture<R> operation(OperationManager<BigInteger, R> operationManager, Operation
            operation, BigInteger key) {
        CompletableFuture<R> operationState = operationManager.putIfAbsent(key);

        if (operationState != null)
            return operationState;

        operationState = operationManager.get(key);

        NodeInfo destination;
        try {
            destination = fingerTable.lookup(key).get();
        } catch (InterruptedException | ExecutionException e) {
            fingerTable.onLookupFailed(key);
            operationState.completeExceptionally(e);
            return operationState;
        }

        try {
            Mailman.sendOperation(destination, operation);
        } catch (IOException e) {
            operationState.completeExceptionally(e);
            return operationState;
        }

        return operationState;
    }

    CompletableFuture<Boolean> remove(BigInteger key) {
        return operation(ongoingDeletes, new RemoveOperation(self, key), key);
    }

    public boolean removeValue(BigInteger key) {
        return dht.removeLocally(key);
    }
}
