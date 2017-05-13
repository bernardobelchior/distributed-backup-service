package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;

public class LookupOperation implements Operation {
    private BigInteger key;
    private final NodeInfo origin;
    private final NodeInfo lastNode;
    private boolean lastHop = false;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        lastNode = origin;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.print("Looking up key " + key + ". ");

        try {
            FingerTable fingerTable = currentNode.getFingerTable();

            if (lastHop || fingerTable.isEmpty()) {
                fingerTable.setPredecessor(lastNode);

                if (fingerTable.isEmpty())
                    fingerTable.updateSuccessor(0, lastNode);

                System.out.println("I own it! Sending my info...");
                Mailman.sendObject(origin, new LookupResultOperation(currentNode.getInfo(), key));
                return;
            }

            if (currentNode.keyBelongsToSuccessor(key)) {
                lastHop = true;
                System.out.println("It should belong to my successor, forwarding to him.");
            } else {
                System.out.println("It does not belong to my successor. Forwarding to next best node...");
            }

            currentNode.forwardToNextBestNode(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigInteger getKey() {
        return key;
    }
}
