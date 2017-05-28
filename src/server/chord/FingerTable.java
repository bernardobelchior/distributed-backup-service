package server.chord;

import server.communication.Mailman;
import server.communication.OperationManager;
import server.communication.operations.LookupOperation;
import server.communication.operations.NotifyOperation;
import server.exceptions.KeyNotFoundException;
import server.utils.SynchronizedFixedLinkedList;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.*;

import static server.chord.Node.MAX_NODES;
import static server.chord.Node.OPERATION_MAX_FAILED_ATTEMPTS;
import static server.utils.Utils.*;

public class FingerTable {
    private static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));
    private static final int NUM_SUCCESSORS = 5;
    static final int LOOKUP_TIMEOUT = 1000; // In milliseconds

    final OperationManager<BigInteger, NodeInfo> ongoingLookups = new OperationManager<>();

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
     * @param finger
     */
    private void setFinger(int index, NodeInfo finger) {
        fingers[index] = finger;
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
        sb.append("Index  Key    ID\n");

        for (int i = 0; i < fingers.length; i++) {
            sb.append(i);
            sb.append("      ");
            sb.append(Integer.remainderUnsigned((int) (self.getId() + Math.pow(2, i)), MAX_NODES));
            sb.append("     ");
            sb.append(fingers[i] == null
                    ? "null"
                    : fingers[i].getId());
            sb.append("\n");
        }

        sb.append("\nSuccessors:\n");
        sb.append("Index      ID\n");

        for (int i = 0; i < successors.size(); i++) {
            sb.append(i);
            sb.append("      ");
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

        for (int i = 0; i < FINGER_TABLE_SIZE; i++)
            getFinger(i);
    }

    private static int getFingerKey(NodeInfo node, int index) {
        return addToNodeId(node.getId(), (int) Math.pow(2, index));
    }

    private void getFinger(int index) {
        BigInteger keyToLookup = BigInteger.valueOf(getFingerKey(self, index));

        int attempts = OPERATION_MAX_FAILED_ATTEMPTS;
        while (attempts > 0) {
            try {
                CompletableFuture<Void> fingerLookup = lookup(keyToLookup)
                        .thenAcceptAsync(this::informAboutExistence, lookupThreadPool);

                fingerLookup.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
                break;
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                attempts--;
            }
        }

        if (attempts <= 0)
            ongoingLookups.operationFailed(keyToLookup, new KeyNotFoundException());
    }

    /**
     * Check if a given node should replace any of the finger table's nodes
     *
     * @param node node being compared
     */
    private void updateFingerTable(NodeInfo node) {
        BigInteger keyEquivalent = BigInteger.valueOf(node.getId());

        for (int i = 0; i < fingers.length; i++) {
            int lower = addToNodeId(self.getId(), (int) Math.pow(2, i) - 1);
            if (between(lower, fingers[i].getId(), keyEquivalent) && !fingers[i].equals(node) && !self.equals(node))
                setFinger(i, node);

        }
    }

    void informAboutExistence(NodeInfo node) {
        updatePredecessor(node);
        updateSuccessors(node);
        updateFingerTable(node);
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

    private void updateSuccessors(NodeInfo node) {
        if (node.equals(self))
            return;

        /* Find, if any, the first successor that should be after the node being checked
         * Shift all the nodes in the array a position forwards and
         * Insert the node in the correct position */
        synchronized (successors) {
            NodeInfo successor;
            int nodeKey;
            if (successors.size() > 0) {
                successor = successors.get(0);
                nodeKey = node.getId();

                /* If the node is already in the successor list, then do nothing */
                if (node.equals(successor))
                    return;

                /* If the node belongs between two successors, then add it to that position */
                if (between(self, successor, nodeKey)) {
                    successors.add(0, node);
                /* Send a lookup to successor. This will notify that I am his new predecessor. */
                    lookupFrom(BigInteger.valueOf(successor.getId()), successor);
                    return;
                }
            }

            for (int i = 1; i < successors.size(); i++) {
                NodeInfo lowerNode = successors.get(i - 1);
                successor = successors.get(i);
                nodeKey = node.getId();

                /* If the node is already in the successor list, then do nothing */
                if (node.equals(successor))
                    return;

                /* If the node belongs between two successors, then add it to that position */
                if (between(lowerNode, successor, nodeKey)) {
                    successors.add(i, node);
                    return;
                }
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
            if (fingers[i].equals(node)) {
                setFinger(i, self);
                getFinger(i);
            }
    }

    void informPredecessorOfFailure(NodeInfo node) {
        if (predecessor.equals(node))
            setPredecessor(null);
    }

    /**
     * Informs this node's successor about the failure of a node. This function will iterate through all of this node's
     * successors and removes the node that failed, starting a new lookup operation to find a new successor.
     *
     * @param node Node that failed.
     * @return Index of the removed node, or -1 if none was removed.
     */
    int informSuccessorsOfFailure(NodeInfo node) {
        /* Inform my second successor that its predecessor failed. */
        if (successors.get(0).equals(node)) {
            try {
                Mailman.sendOperation(successors.get(1), new NotifyOperation(self));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.err.println("Node with ID " + node.getId() + " failed!");
        int removedIndex = successors.remove(node);

        if (removedIndex == 0) {
            lookup(BigInteger.valueOf(addToNodeId(self.getId(), 1)));
        } else if (removedIndex > 0) {
            lookup(BigInteger.valueOf(addToNodeId(successors.last().getId(), 1)));
        }

        return removedIndex;
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
        CompletableFuture<NodeInfo> lookupResult = ongoingLookups.putIfAbsent(key);

        if (lookupResult != null)
            return lookupResult;

        lookupResult = ongoingLookups.get(key);

        try {
            Mailman.sendOperation(nodeToLookup, new LookupOperation(this, self, key, nodeToLookup));
        } catch (Exception e) {
            ongoingLookups.operationFailed(key, e);
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

            int attempts = OPERATION_MAX_FAILED_ATTEMPTS;
            while (attempts > 0) {
                try {
                    successorLookup.get(LOOKUP_TIMEOUT, TimeUnit.MILLISECONDS);
                    break;
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    /* If the lookup did not complete correctly */
                    attempts--;
                }
            }

            if (attempts <= 0)
                return false;

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

    /**
     * Get the node's successor's predecessor, check if it is not the current node
     * and notify the successor of this node's existence
     */
    private void stabilizeSuccessors() {
        lookup(getSuccessorKey(self));

        for (int i = 1; i < successors.size(); i++)
            lookup(getSuccessorKey(successors.get(i)));

        notifySuccessor();
    }

    private void notifySuccessor() {
        NotifyOperation notification = new NotifyOperation(self);

        try {
            Mailman.sendOperation(getSuccessor(), notification);
        } catch (Exception e) {
            System.err.println("Unable to notify successor");
        }
    }

    void stabilizationProtocol() {
        if (!hasSuccessors())
            return;

        stabilizeSuccessors();
        fill();
    }

    SynchronizedFixedLinkedList<NodeInfo> getSuccessors() {
        return successors;
    }
}
