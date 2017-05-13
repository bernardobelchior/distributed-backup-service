package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;

public class LookupOperation implements Operation {
    private BigInteger key;
    private NodeInfo origin;
    private boolean isLastHop = false;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.print("Looking up key " + key + ". ");

        try {
            if (isLastHop || currentNode.emptyFingerTable()) {
                System.out.println("I own it! Sending my info...");
                Mailman.sendObject(origin, new LookupResultOperation(currentNode.getInfo(), key));
                return;
            }

            if (currentNode.keyBelongsToSuccessor(key)) {
                System.out.print("It should belong to my successor.");
                isLastHop = true;
            }

            System.out.println("Forwarding to next best node...");
            currentNode.forwardToNextBestNode(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigInteger getKey() {
        return key;
    }
}
