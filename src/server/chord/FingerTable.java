package server.chord;

import java.net.InetAddress;

import static server.chord.Node.MAX_NODES;
import static server.chord.Node.between;

public class FingerTable {
    public static final int FINGER_TABLE_SIZE = (int) (Math.log(MAX_NODES) / Math.log(2));

    private NodeInfo[] table;

    public FingerTable(FingerTable ft) {
        this.table = ft.getTable().clone();
    }

    public FingerTable(NodeInfo selfInfo) {
        table = new NodeInfo[FINGER_TABLE_SIZE];

        for (int i = 0; i < table.length; i++)
            table[i] = selfInfo;
    }

    public void update(int index, NodeInfo nodeInfo) {
        table[index] = nodeInfo;
    }

    public NodeInfo[] getTable() {
        return table;
    }

    /**
     * Get the closest preceding node of the key
     * @param selfID the current node's Id
     * @param key the key being searched
     * @return NodeInfo of the closest preceding node
     */
    public NodeInfo lookup(int selfID, int key) {
        for (int i = table.length - 1; i >= 0; i++) {
            if(between(selfID,key,table[i].getId()))
                return table[i];
        }
        return null;
    }
}
