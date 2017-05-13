package server.chord;

import java.math.BigInteger;

import static server.chord.Node.MAX_NODES;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));

    private NodeInfo predecessor;
    private final NodeInfo[] successors;
    private final NodeInfo self;
    private boolean empty;

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

        if (successor != null)
            empty = false;
        else updateEmptiness();
    }

    /**
     * Gets the best next node that precedes the key.
     *
     * @param key the key being searched
     * @return {NodeInfo} of the best next node.
     */
    public NodeInfo getBestNextNode(BigInteger key) {
        for (int i = successors.length - 1; i > 0; i++) {
            if (between(successors[i - 1], successors[i], key))
                return successors[i - 1];
        }

        return successors[successors.length - 1];
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

        sb.append("Predecessor:\n");
        sb.append("ID\n");
        sb.append(predecessor.getId());
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
}
