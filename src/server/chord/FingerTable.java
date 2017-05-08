package server.chord;

import java.math.BigInteger;

import static server.chord.Node.MAX_NODES;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));

    private NodeInfo predecessor;
    private NodeInfo[] successors;
    private NodeInfo self;

    public FingerTable(NodeInfo self) {
        this.self = self;
        predecessor = self;
        successors = new NodeInfo[FINGER_TABLE_SIZE];

        for (int i = 0; i < successors.length; i++)
            successors[i] = self;
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public static boolean between(NodeInfo lower, NodeInfo upper, BigInteger key) {
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
    public static boolean between(int lower, int upper, BigInteger key) {
        int keyOwner = Integer.remainderUnsigned(key.intValueExact(), MAX_NODES);

        if (lower < upper)
            return keyOwner > lower && keyOwner <= upper;
        else
            return keyOwner > lower || keyOwner <= upper;
    }

    public void update(int index, NodeInfo nodeInfo) {
        successors[index] = nodeInfo;
    }

    public NodeInfo[] getSuccessors() {
        return successors;
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

    public boolean inRange(BigInteger key) {
        return between(predecessor, self, key);
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
            sb.append(successors[i].getId());
            sb.append("\n");
        }

        return sb.toString();
    }
}
