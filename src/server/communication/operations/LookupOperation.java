package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;

import static server.chord.DistributedHashTable.MAXIMUM_HOPS;
import static server.chord.Node.MAX_NODES;

public class LookupOperation implements Operation {
    private BigInteger key;
    private final NodeInfo origin;
    private NodeInfo lastNode;
    private boolean reachedDestination = false;
    private int timeToLive;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        lastNode = origin;
        this.key = key;
        timeToLive = MAXIMUM_HOPS;
    }

    @Override
    public void run(Node currentNode) {
        if(--timeToLive < 0)
            return;

        System.out.println("Looking up key " + /*DatatypeConverter.printHexBinary(key.toByteArray())*/ key + " from node " + origin.getId() + ". Last node was: " + lastNode.getId() + ". Reached destination: " + reachedDestination);

        FingerTable fingerTable = currentNode.getFingerTable();

        NodeInfo senderNode = lastNode;
        lastNode = currentNode.getInfo();

        try {
            if (reachedDestination) {
                Mailman.sendOperation(origin, new LookupResultOperation(currentNode.getInfo(), key));

                fingerTable.updateFingerTable(origin);
                fingerTable.updateSuccessors(origin);
                fingerTable.updateFingerTable(senderNode);
                fingerTable.updateSuccessors(senderNode);
                return;
            }

            if (currentNode.keyBelongsToSuccessor(key))
                reachedDestination = true;

            NodeInfo nextBestNode = currentNode.getNextBestNode(key);

            if (nextBestNode == null || currentNode.getInfo().equals(nextBestNode))
                nextBestNode = currentNode.getSuccessor();

            Mailman.sendOperation(nextBestNode, this);
            System.out.format("Redirected message to next best node, with ID %d\n", nextBestNode.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }

        fingerTable.updateFingerTable(origin);
        fingerTable.updateFingerTable(senderNode);
        fingerTable.updateSuccessors(origin);
        fingerTable.updateSuccessors(senderNode);


    }
}
