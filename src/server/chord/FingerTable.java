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
    private boolean empty;
    private CompletableFuture<Void> fingerTableInitialized = new CompletableFuture<>();

    public FingerTable(NodeInfo self) {
        this.self = self;
        predecessor = null;
        successors = new NodeInfo[FINGER_TABLE_SIZE];
        empty = true;

        for (int i = 0; i < successors.length; i++)
            successors[i] = null;
    }

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

    public void updateSuccessor(int index, NodeInfo successor) {
        System.out.println("Updated successors[" + index + "] with " + successor);
        successors[index] = successor;

        if (successor != null) {
            empty = false;
            fingerTableInitialized.complete(null);
        } else updateEmptiness();
    }

    /**
     * Gets the best next node that precedes the key.
     *
     * @param key the key being searched
     * @return {NodeInfo} of the best next node.
     */
    public NodeInfo getBestNextNode(BigInteger key) {
        NodeInfo bestNextNode = successors[0];

        for (int i = 1; i < successors.length; i++) {
            if (successors[i] == null || !between(bestNextNode, successors[i], key))
                break;
            else
                bestNextNode = successors[i];
        }

        return bestNextNode;
    }

    public void setPredecessor(NodeInfo predecessor) {
        System.out.println("Updated predecessor with: " + predecessor);
        this.predecessor = predecessor;

        if (predecessor != null)
            empty = false;
        else updateEmptiness();
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

    private void updateEmptiness() {
        if (predecessor != null) {
            empty = false;
            return;
        }

        for (NodeInfo successor : successors)
            if (successor != null) {
                empty = false;
                return;
            }

        empty = true;
    }

    public boolean isEmpty() {
        return empty;
    }

    private int distanceToNode(BigInteger key) {
        int remainder = key.remainder(BigInteger.valueOf(MAX_NODES)).intValue();

        return Integer.remainderUnsigned(MAX_NODES + remainder - self.getId(), MAX_NODES);
    }

    public void updateFingerTable(BigInteger key, NodeInfo owner) {
        /* Updates successors */
        double index = Math.log(distanceToNode(key)) / Math.log(2);

        /* if the index is an integer */
        if (index == (int) index)
            successors[(int) index] = owner;
    }

    public void waitForTableInitialization() throws ExecutionException, InterruptedException {
        fingerTableInitialized.get();
    }

    public void fill(Node currentNode) {
        for (int i = 1; i < FINGER_TABLE_SIZE; i++) {
            /* (NodeId + 2^i) mod MAX_NODES */
            BigInteger keyToLookup = BigInteger.valueOf(addToNodeId(self.getId(), (int) Math.pow(2, i)));

            try {
                /* If the key corresponding to the ith row of the finger table stands between me and my successor,
                * it means that successors[i] is still my successor. If it is not, look for the corresponding node. */
                if (between(self, successors[0], keyToLookup))
                    successors[i] = successors[0];
                else
                    currentNode.lookup(keyToLookup);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
