package server.chord;

import static server.chord.Node.MAX_NODES;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));

    private final NodeInfo[] table = new NodeInfo[FINGER_TABLE_SIZE];

    public FingerTable(NodeInfo self) {
        for (int i = 0; i < FINGER_TABLE_SIZE; i++)
            table[i] = self;
    }

    public void update(int index, NodeInfo entry) {
        table[index] = entry;
    }
}
