package server.chord;

import server.utils.SynchronizedFixedLinkedList;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static server.chord.Node.MAX_NODES;
import static server.utils.Utils.addToNodeId;
import static server.utils.Utils.between;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));
    public static final int NUM_SUCCESSORS = 5;

    private NodeInfo predecessor;
    private final NodeInfo[] fingers;
    private final SynchronizedFixedLinkedList<NodeInfo> successors;
    private final NodeInfo self;

    public FingerTable(NodeInfo self) {
        this.self = self;
        predecessor = self;
        fingers = new NodeInfo[FINGER_TABLE_SIZE];
        successors = new SynchronizedFixedLinkedList<>(NUM_SUCCESSORS);

        for (int i = 0; i < fingers.length; i++)
            fingers[i] = self;
    }

    /**
     * Check if a given key belongs to this node's successor, that is,
     * if the key is between this node and the successor
     *
     * @param key key being checked
     * @return true if the key belongs to the node's successor
     */
    public boolean keyBelongsToSuccessor(BigInteger key) {
        return between(self, fingers[0], key);
    }


    /**
     * @param index
     * @param successor
     */
    public void setFinger(int index, NodeInfo successor) {
        System.out.println("Updated fingers[" + index + "] with " + successor);
        fingers[index] = successor;
    }

    /**
     * Gets the next best node that precedes the key.
     *
     * @param key the key being searched
     * @return {NodeInfo} of the best next node.
     */
    public NodeInfo getNextBestNode(BigInteger key) {

        int keyOwner = Integer.remainderUnsigned(key.intValueExact(), MAX_NODES);
        for (int i = fingers.length - 1; i >= 0; i--) {
            if (fingers[i].getId() != keyOwner && between(self.getId(), keyOwner, fingers[i].getId()) && !fingers[i].equals(self))
                return fingers[i];
        }

        return successors.get(0);
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
        sb.append("Index\t\t\tID\n");

        for (int i = 0; i < fingers.length; i++) {
            sb.append(i);
            sb.append("\t\t\t");
            sb.append(fingers[i] == null
                    ? "null"
                    : fingers[i].getId());
            sb.append("\n");
        }

        sb.append("Successors:\n");
        sb.append("Index\t\t\tID\n");

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
     *
     * @param currentNode node the finger table belongs to
     * @throws Exception
     */
    public void fill(Node currentNode) throws IOException {
        if (fingers[0] == null)
            return;

        for (int i = 1; i < FINGER_TABLE_SIZE; i++) {

            /* (NodeId + 2^i) mod MAX_NODES */
            BigInteger keyToLookup = BigInteger.valueOf(addToNodeId(self.getId(), (int) Math.pow(2, i)));

            try {
                /*
                 * If the key corresponding to the ith row of the finger table stands between me and my successor,
                 * it means that fingers[i] is still my successor. If it is not, look for the corresponding node.
                 */
                if (fingers[i] != null && between(self, fingers[0], keyToLookup))
                    fingers[i] = fingers[0];
                else {
                    int index = i;
                    CompletableFuture<Void> fingerLookup = currentNode.lookup(keyToLookup).thenAcceptAsync(
                            finger -> setFinger(index, finger),
                            currentNode.getThreadPool());

                    System.out.println("i = " + i);
                    fingerLookup.get(400, TimeUnit.MILLISECONDS);

                    if (fingerLookup.isCancelled() || fingerLookup.isCompletedExceptionally())
                        throw new IOException("Could not find finger" + i);
                }
            } catch (TimeoutException | IOException | InterruptedException | ExecutionException e) {
                throw new IOException("Could not find finger " + i);
            }
        }
    }

    /**
     * Check if a given node should replace any of the finger table's nodes
     *
     * @param node node being compared
     */
    public void updateFingerTable(NodeInfo node) {
        BigInteger keyEquivalent = BigInteger.valueOf(node.getId());

        for (int i = 0; i < fingers.length; i++)
            if (between(addToNodeId(self.getId(), (int) Math.pow(2, i)), fingers[i].getId(), keyEquivalent)) {

                if (i == 0)
                    setSuccessor(node, 0); //already sets finger[0] to the node
                else
                    fingers[i] = node;
            }
    }

    /**
     * Get this node's successor
     *
     * @return NodeInfo for the successor
     */
    public NodeInfo getSuccessor() {
        return (successors.get(0) != null ? successors.get(0) : fingers[0]);
    }

    public NodeInfo getNthSuccessor(int index) throws IndexOutOfBoundsException {
        return successors.get(index);
    }

    public void setSuccessor(NodeInfo successor, int index) {
        successors.set(index, successor);
        fingers[index] = successor;
    }

    /**
     * Get this node's predecessor
     *
     * @return NodeInfo for the predecessor
     */
    public NodeInfo getPredecessor() {
        return predecessor;
    }

    /**
     * Set the node's predecessor without checking
     * Use only if needed (e.g. setting the predecessor to null)
     * See updatePredecessor()
     *
     * @param predecessor new predecessor
     */
    public void setPredecessor(NodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    /**
     * Check if a given node should be this node's predecessor and sets it if it should.
     *
     * @param node Node being checked
     */
    public boolean updatePredecessor(NodeInfo node) {
        if (node == null || node.equals(self))
            return false;

        if (predecessor == null) {
            predecessor = node;
            return true;
        }

        if (between(predecessor, self, node.getId())) {
            predecessor = node;
            return true;
        }

        return false;
    }

    public void updateSuccessors(NodeInfo node) {
        if (node.equals(self))
            return;

        NodeInfo lowerNode = self;

        /*
         * Find, if any, the first successor that should be after the node being checked
         * Shift all the nodes in the array a position forwards and
         * Insert the node in the correct position
         */
        ListIterator<NodeInfo> iterator = successors.listIterator();
        while (iterator.hasNext()) {
            NodeInfo successor = iterator.next();
            if (node.equals(successor))
                break;

            int nodeKey = node.getId();

            if (between(lowerNode, successor, nodeKey)) {
                iterator.add(node);
                break;
            }

            lowerNode = successor;
        }

        if (successors.size() < NUM_SUCCESSORS)
            successors.add(node);
    }

    public void deleteSuccessor() {
        successors.popLast();
        fingers[0] = successors.get(0);
    }

    public void informAboutNode(NodeInfo node) {
        updateSuccessors(node);
        updateFingerTable(node);
        updatePredecessor(node);
    }
}
