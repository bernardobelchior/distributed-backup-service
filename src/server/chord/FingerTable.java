package server.chord;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static server.Utils.addToNodeId;
import static server.chord.Node.MAX_NODES;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));

    private NodeInfo predecessor;
    private final NodeInfo[] successors;
    private final NodeInfo self;

    public FingerTable(NodeInfo self) {
        this.self = self;
        predecessor = self;
        successors = new NodeInfo[FINGER_TABLE_SIZE];

        for (int i = 0; i < successors.length; i++)
            successors[i] = self;
    }

    /**
     * Check if a given key belongs to this node's successor, that is,
     * if the key is between this node and the successor
     *
     * @param key key being checked
     * @return true if the key belongs to the node's successor
     */
    public boolean keyBelongsToSuccessor(BigInteger key) {
        return between(self, successors[0], key);
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public boolean between(NodeInfo lower, NodeInfo upper, BigInteger key) {
        return between(lower.getId(), upper.getId(), key);
    }


    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public boolean between(int lower, int upper, BigInteger key) {
        int keyOwner = Integer.remainderUnsigned(key.intValueExact(), MAX_NODES);

        if (lower < upper)
            return keyOwner > lower && keyOwner <= upper;
        else
            return keyOwner > lower || keyOwner <= upper;
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public boolean between(int lower, int upper, int key) {

        if (lower < upper)
            return key > lower && key <= upper;
        else
            return key > lower || key <= upper;
    }

    /**
     * @param index
     * @param successor
     */
    public void setFinger(int index, NodeInfo successor) {
        System.out.println("Updated successors[" + index + "] with " + successor);
        successors[index] = successor;
    }

    /**
     * Gets the best next node that precedes the key.
     *
     * @param key the key being searched
     * @return {NodeInfo} of the best next node.
     */
    public NodeInfo getNextBestNode(BigInteger key) {

        int keyOwner = Integer.remainderUnsigned(key.intValueExact(), MAX_NODES);
        for (int i = successors.length - 1; i >= 0; i--) {
            if (between(self.getId(), keyOwner, successors[i].getId()))
                return successors[i];
        }

        return self;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Predecessor ID: ");
        sb.append(predecessor == null
                ? "null"
                : predecessor.getId());
        sb.append("\n\n");
        sb.append("Successors:\n");
        sb.append("Index\t\t\tID\n");

        for (int i = 0; i < successors.length; i++) {
            sb.append(i);
            sb.append("\t\t\t");
            sb.append(successors[i] == null
                    ? "null"
                    : successors[i].getId());
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
    public void fill(Node currentNode) throws Exception {
        for (int i = 1; i < FINGER_TABLE_SIZE; i++) {

            /* (NodeId + 2^i) mod MAX_NODES */
            BigInteger keyToLookup = BigInteger.valueOf(addToNodeId(self.getId(), (int) Math.pow(2, i)));

            try {
                /*
                 * If the key corresponding to the ith row of the finger table stands between me and my successor,
                 * it means that successors[i] is still my successor. If it is not, look for the corresponding node.
                 */
                if (between(self, successors[0], keyToLookup))
                    successors[i] = successors[0];
                else {
                    int index = i;
                    CompletableFuture<Void> fingerLookup = currentNode.lookup(keyToLookup).thenAcceptAsync(
                            finger -> setFinger(index, finger),
                            currentNode.getThreadPool());

                    fingerLookup.get();
                    if (fingerLookup.isCancelled() || fingerLookup.isCompletedExceptionally())
                        throw new Exception("Could not find finger" + i);
                }
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new Exception("Could not find successor " + i);
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

        for (int i = 0; i < successors.length; i++)
            if (between(addToNodeId(self.getId(), (int) Math.pow(2, i)), successors[i].getId(), keyEquivalent))
                successors[i] = node;
    }

    /**
     * Get this node's successor
     *
     * @return NodeInfo for the successor
     */
    public NodeInfo getSuccessor() {
        return successors[0];
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
     * Use only if needed. See updatePredecessor()
     *
     * @param predecessor new predecessor
     */
    public void setPredecessor(NodeInfo predecessor) {
        this.predecessor = predecessor;
    }

    /**
     * Check if a given node should be this node's predecessor
     *
     * @param node node being compared
     */
    public void updatePredecessor(NodeInfo node) {
        BigInteger keyEquivalent = BigInteger.valueOf(node.getId());
        if (between(predecessor, self, keyEquivalent))
            predecessor = node;
    }

}
