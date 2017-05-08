package server.chord;

import java.math.BigInteger;

import static server.chord.Node.MAX_NODES;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));

    private NodeInfo[] table;
    private NodeInfo self;

    public FingerTable(NodeInfo self, FingerTable table) {
        this.self = self;
        this.table = table.getTable().clone();
    }

    public FingerTable(NodeInfo self) {
        this.self = self;
        table = new NodeInfo[FINGER_TABLE_SIZE];

        for (int i = 0; i < table.length; i++)
            table[i] = self;
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
        table[index] = nodeInfo;
    }

    public NodeInfo[] getTable() {
        return table;
    }

    /**
     * Gets the best next node that precedes the key.
     *
     * @param key the key being searched
     * @return {NodeInfo} of the best next node.
     */
    public NodeInfo getBestNextNode(BigInteger key) {
        for (int i = table.length - 1; i > 0; i++) {
            if (between(table[i - 1], table[i], key))
                return table[i - 1];
        }

        return table[table.length - 1];
    }
}
