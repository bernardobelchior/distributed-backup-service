package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class RemapKeysOperation extends Operation {
    private ConcurrentHashMap<BigInteger, byte[]> keys;

    RemapKeysOperation(NodeInfo origin, ConcurrentHashMap<BigInteger, byte[]> keys) {
        super(origin);
        this.keys = keys;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.remappedKeys(keys);
    }
}
