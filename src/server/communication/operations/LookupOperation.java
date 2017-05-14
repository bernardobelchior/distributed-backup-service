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
        System.out.println("Looking up key " + key + " from node " + origin.getId() + ". Last node was: " + lastNode.getId() + ". Is last hop? " + lastHop);

        try {
            FingerTable fingerTable = currentNode.getFingerTable();

            if (lastHop || fingerTable.isEmpty()) {
                if (fingerTable.isEmpty())
                    fingerTable.updateSuccessor(0, lastNode);

                fingerTable.setPredecessor(lastNode);

                LookupResultOperation lookupResultOperation = new LookupResultOperation(currentNode.getInfo(), key);

                /* If the current node is the origin node, then just complete the lookup.
                 * Otherwise, send it to the node which requested the lookup. */
                if (currentNode.getInfo().equals(origin))
                    lookupResultOperation.run(currentNode);
                else
                    Mailman.sendObject(origin, new LookupResultOperation(currentNode.getInfo(), key));

                return;
            }

            if (currentNode.keyBelongsToSuccessor(key))
                lastHop = true;

            currentNode.forwardToNextBestNode(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigInteger getKey() {
        return key;
    }
}
