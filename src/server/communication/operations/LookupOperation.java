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
    private NodeInfo lastNode;
    private boolean reachedDestination = false;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        lastNode = origin;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Looking up key " + key + " from node " + origin.getId() + ". Last node was: " + lastNode.getId() + ". Reached destination: " + reachedDestination);

        FingerTable fingerTable = currentNode.getFingerTable();
        fingerTable.updateFingerTable(origin);
        fingerTable.updateFingerTable(lastNode);

        lastNode = currentNode.getInfo();

        try {
            if (reachedDestination) {
                System.out.println("Reached Destination");
                LookupResultOperation lookupResultOperation = new LookupResultOperation(currentNode.getInfo(), key);

                /* If the key belongs to the origin node, then just complete the lookup.
                 * Otherwise, send it to the node which requested the lookup. */
                if (currentNode.getInfo().equals(origin)) {
                    System.out.println("Key belongs to the origin node");
                    lookupResultOperation.run(currentNode);
                }
                else
                    Mailman.sendObject(origin, new LookupResultOperation(currentNode.getInfo(), key));

                return;
            }

            if (currentNode.keyBelongsToSuccessor(key))
                reachedDestination = true;

            NodeInfo nextBestNode = currentNode.getNextBestNode(key);

            if (currentNode.getInfo().equals(nextBestNode)) {
                nextBestNode = currentNode.getSuccessor();
                System.out.println("Reached desired destination");
            }
//                new LookupResultOperation(currentNode.getInfo(), key).run(currentNode);
//            else
                Mailman.sendObject(nextBestNode, this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKey() {
        return key.toString();
    }
}
