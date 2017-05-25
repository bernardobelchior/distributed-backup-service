package server.chord;

import server.communication.Mailman;
import server.communication.OperationManager;
import server.communication.operations.LookupOperation;
import server.communication.operations.NotifyOperation;
import server.communication.operations.PingOperation;
import server.exceptions.KeyNotFoundException;
import server.utils.SynchronizedFixedLinkedList;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.*;

import static server.chord.Node.MAX_NODES;
import static server.utils.Utils.*;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));
    public static final int NUM_SUCCESSORS = 5;
    static final int LOOKUP_TIMEOUT = 2000; // In milliseconds

    public final OperationManager<BigInteger, NodeInfo> ongoingLookups = new OperationManager<>();
    public final OperationManager<BigInteger, NodeInfo> ongoingPings = new OperationManager<>();

    private NodeInfo predecessor;
    private final NodeInfo[] fingers;
    private final SynchronizedFixedLinkedList<NodeInfo> successors;
    private final NodeInfo self;
    private final ExecutorService lookupThreadPool = Executors.newFixedThreadPool(10);

    FingerTable(NodeInfo self) {
        this.self = self;
        setPredecessor(self);
        fingers = new NodeInfo[FINGER_TABLE_SIZE];
        successors = new SynchronizedFixedLinkedList<>(NUM_SUCCESSORS);

        for (int i = 0; i < fingers.length; i++)
            setFinger(i, self);
    }

    /**
     * @param index
     * @param successor
     */
    private void setFinger(int index, NodeInfo successor) {
        fingers[index] = successor;
    }

    /**
     * Gets the next best node that precedes the key.
     *
     * @param key the key being searched
     * @return {NodeInfo} of the best next node.
     */
    NodeInfo getNextBestNode(BigInteger key) {

        int keyOwner = Integer.remainderUnsigned(key.intValue(), MAX_NODES);
        for (int i = fingers.length - 1; i >= 0; i--) {
            if (between(self.getId(), keyOwner, fingers[i].getId()))
                return fingers[i];
        }

        return getSuccessor();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Predecessor ID: ");
        sb.append(predecessor == null
                ? "null"
                : predecessor.getId());
        sb.append("\n\n");
        sb.append("Finger Table:\n");
        sb.append("Index\tKey\t\tID\n");

        for (int i = 0; i < fingers.length; i++) {
            sb.append(i);
            sb.append("\t\t");
            sb.append(Integer.remainderUnsigned((int) (self.getId() + Math.pow(2, i)), MAX_NODES));
            sb.append("\t\t");
            sb.append(fingers[i] == null
                    ? "null"
                    : fingers[i].getId());
            sb.append("\n");
        }

        sb.append("\nSuccessors:\n");
        sb.append("Index\t\tID\n");

        for (int i = 0; i < successors.size(); i++) {
            sb.append(i);
            sb.append("\t\t\t");
            sb.append(successors.get(i) == null
                    ? "null"
                    : successors.get(i).getId());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Fills the node's finger table
     */
    void fill() {
        if (!hasSuccessors())
            return;

        for (int i = 1; i < FINGER_TABLE_SIZE; i++) {
            getFinger(i);
        }
    }

    private static int getFingerKey(NodeInfo node, int index) {
        return addToNodeId(node.getId(), (int) Math.pow(2, index));
    }

    private boolean getFinger(int index) {
        BigInteger keyToLookup = BigInteger.valueOf(getFingerKey(self, index));

        /* Check if the finger belongs to the successors. If it does, then we don't need to look it up. */
        /* int lower = addToNodeId(self.getId(), -1);
        FIXME: Useful optimization, but has a problem when the current node ID stands between successors
        for (int i = 0; i < successors.size(); i++) {
            if (between(lower, successors.get(i).getId(), keyToLookup)) {
                setFinger(index, successors.get(i));
                return true;
            }
        }*/

        try {
            CompletableFuture<Void> fingerLookup = lookup(keyToLookup).thenAcceptAsync(
                    finger -> setFinger(index, finger),
                    lookupThreadPool);

            fingerLookup.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            ongoingLookups.operationFailed(keyToLookup, new KeyNotFoundException());
            setFinger(index, self);
            return false;
        }

        return true;
    }

    /**
     * Check if a given node should replace any of the finger table's nodes
     *
     * @param node node being compared
     */
    void updateFingerTable(NodeInfo node) {
        BigInteger keyEquivalent = BigInteger.valueOf(node.getId());

        for (int i = 0; i < fingers.length; i++) {
            int lower = addToNodeId(self.getId(), (int) Math.pow(2, i) - 1);
            if (between(lower, fingers[i].getId(), keyEquivalent)) {
                if (!fingers[i].equals(node))
                    setFinger(i, node);
            }
        }
    }

    /**
     * Get this node's successor
     *
     * @return NodeInfo for the successor
     */
    public NodeInfo getSuccessor() {
        return (!successors.isEmpty() ? successors.get(0) : self);
    }

    NodeInfo getNthSuccessor(int index) throws IndexOutOfBoundsException {
        return successors.get(index);
    }

    /**
     * Get this node's predecessor
     *
     * @return NodeInfo for the predecessor
     */
    NodeInfo getPredecessor() {
        return predecessor;
    }

    /**
     * Set the node's predecessor without checking
     * Use only if needed (e.g. setting the predecessor to null)
     * See updatePredecessor()
     *
     * @param predecessor new predecessor
     */
    private void setPredecessor(NodeInfo predecessor) {
        System.err.println("Predecessor set to " + (predecessor == null ? "null" : predecessor.getId()));
        this.predecessor = predecessor;
    }

    /**
     * Check if a given node should be this node's predecessor and sets it if it should.
     *
     * @param node Node being checked
     */
    boolean updatePredecessor(NodeInfo node) {
        if (node == null || node.equals(self))
            return false;

        if (predecessor == null) {
            setPredecessor(node);
            return true;
        }

        if (between(predecessor, self, node.getId())) {
            setPredecessor(node);
            return true;
        }

        return false;
    }

    void updateSuccessors(NodeInfo node) {
        if (node.equals(self))
            return;

        NodeInfo lowerNode = self;

        /*
         * Find, if any, the first successor that should be after the node being checked
         * Shift all the nodes in the array a position forwards and
         * Insert the node in the correct position
         */
        synchronized (successors) {
            for (int i = 0; i < successors.size(); i++) {
                NodeInfo successor = successors.get(i);
                if (node.equals(successor))
                    return;

                int nodeKey = node.getId();

                if (between(lowerNode, successor, nodeKey)) {
                    successors.add(i, node);
                    return;
                }

                lowerNode = successor;
            }
        }

        if (successors.size() < NUM_SUCCESSORS)
            successors.add(node);
    }

    private boolean hasSuccessors() {
        return !successors.isEmpty();
    }

    void informFingersOfFailure(NodeInfo node) {
        for (int i = fingers.length - 1; i >= 0; i--)
            if (fingers[i].equals(node))
                getFinger(i);
    }

    void informPredecessorOfFailure(NodeInfo node) {
        if (predecessor.equals(node))
            setPredecessor(null);
    }

    void informSuccessorsOfFailure(NodeInfo node) {
        /* Inform my second successor that its predecessor failed. */
        if (successors.get(0).equals(node)) {
            try {
                Mailman.sendOperation(successors.get(1), new NotifyOperation(self));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.err.println("Node with ID " + node.getId() + " failed!");
        if (successors.remove(node)) {
            System.err.println("removed");
            if (successors.isEmpty())
                lookup(BigInteger.valueOf(addToNodeId(self.getId(), 1)));
            else
                lookup(BigInteger.valueOf(addToNodeId(successors.last().getId(), 1)));
        }
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
        CompletableFuture<NodeInfo> lookupResult = ongoingLookups.putIfAbsent(key);

        if (lookupResult != null)
            return lookupResult;

        lookupResult = ongoingLookups.get(key);
        try {
            Mailman.sendOperation(nodeToLookup, new LookupOperation(this, self, key, nodeToLookup));
        } catch (IOException e) {
            lookupResult.completeExceptionally(e);
        }

        return lookupResult;
    }

    CompletableFuture<NodeInfo> lookup(BigInteger key) {
        if (keyBelongsToSuccessor(key))
            return lookupFrom(key, getSuccessor());
        else
            return lookupFrom(key, getNextBestNode(key));
    }

    public boolean keyBelongsToSuccessor(BigInteger key) {
        return between(self, getSuccessor(), key);
    }

    boolean findSuccessors(NodeInfo bootstrapperNode) {
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
                successorKey = BigInteger.valueOf(addToNodeId(getNthSuccessor(i).getId(), 1));
            } catch (IndexOutOfBoundsException e) {
                /* This means that there is no Nth successor. As such, we treat it as a normal thing that only
                 * happens when the network has a number of nodes lower than NUM_SUCCESSORS. */
                break;
            }
        }

        return true;
    }

    private boolean pingSuccessor(NodeInfo node) {
        BigInteger successorKey = getSuccessorKey(node);

        CompletableFuture<NodeInfo> ping = pingNode(node);

        try {
            ping.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException e) {
            ongoingLookups.operationFailed(successorKey, new KeyNotFoundException());
            return false;
        } catch (InterruptedException | CancellationException e) {
            ongoingLookups.operationFailed(successorKey, new KeyNotFoundException());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private CompletableFuture<NodeInfo> pingNode(NodeInfo node) {
        BigInteger key = getSuccessorKey(node);
        CompletableFuture<NodeInfo> pingResult = ongoingPings.putIfAbsent(key);
        if (pingResult != null)
            return pingResult;

        pingResult = ongoingPings.get(key);
        try {
            Mailman.sendOperation(node, new PingOperation(self));
        } catch (IOException e) {
            pingResult.completeExceptionally(e);
        }
        return pingResult;
    }

    /**
     * Get the node's successor's predecessor, check if it is not the current node
     * and notify the successor of this node's existence
     */
    private void stabilizeSuccessors() {
        for (int i = 0; i < successors.size(); i++)
            pingSuccessor(successors.get(i));

        notifySuccessor();
    }

    private void notifySuccessor() {
        NotifyOperation notification = new NotifyOperation(self);

        try {
            Mailman.sendOperation(getSuccessor(), notification);
        } catch (IOException e) {
            System.err.println("Unable to notify successor");
        }
    }

    private void stabilizePredecessor() {
        NodeInfo predecessor = getPredecessor();
        if (self.equals(predecessor) || predecessor == null)
            return;

        BigInteger keyEquivalent = BigInteger.valueOf(predecessor.getId());

        CompletableFuture<Void> predecessorLookup = lookup(keyEquivalent).thenAcceptAsync(
                this::updatePredecessor);
        try {
            predecessorLookup.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            System.err.println("Predecessor not responding, deleting reference");
            System.err.println("Could not get key " + addToNodeId(keyEquivalent.intValue(), 0));
            Mailman.state();
            ongoingLookups.operationFailed(keyEquivalent, new KeyNotFoundException());
            setPredecessor(null);
        }
    }

    void stabilizationProtocol() {
        if (!hasSuccessors())
            return;

        stabilizeSuccessors();
        //stabilizePredecessor();
        fill();
    }
}
