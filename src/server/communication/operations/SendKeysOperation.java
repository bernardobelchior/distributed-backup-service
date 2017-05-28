package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class SendKeysOperation extends Operation {
    private ConcurrentHashMap<BigInteger, byte[]> keys;

    public SendKeysOperation(NodeInfo origin, ConcurrentHashMap<BigInteger, byte[]> keys) {
        super(origin);
        this.keys = keys;
    }

    /**
     * This Operation stores in the current node the successor keys.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.storeSuccessorKeys(keys);

        try {
            Mailman.sendOperation(origin, new SendKeysResultOperation(currentNode.getInfo()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
