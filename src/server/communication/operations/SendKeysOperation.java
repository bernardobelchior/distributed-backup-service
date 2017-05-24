package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class SendKeysOperation extends Operation {
    private ConcurrentHashMap<BigInteger, byte[]> keys;

    public SendKeysOperation(NodeInfo origin, ConcurrentHashMap<BigInteger, byte[]> keys) {
        super(origin);
        this.keys = keys;
    }

    @Override
    public void run(Node currentNode) {
        currentNode.receivedSuccessorKeys(keys);

        try {
            Mailman.sendOperation(origin, new SendKeysResultOperation(currentNode.getInfo()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
