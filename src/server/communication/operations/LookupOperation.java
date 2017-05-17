package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import javax.xml.bind.DatatypeConverter;
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
        System.out.println("Looking up key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " from node " + origin.getId() + ". Last node was: " + lastNode.getId() + ". Reached destination: " + reachedDestination);

        FingerTable fingerTable = currentNode.getFingerTable();

        NodeInfo senderNode = lastNode;
        lastNode = currentNode.getInfo();

        try {
            if (reachedDestination) {
                Mailman.sendOperation(origin, new LookupResultOperation(currentNode.getInfo(), key));

                fingerTable.updateFingerTable(origin);
                fingerTable.updateFingerTable(senderNode);
                return;
            }

            if (currentNode.keyBelongsToSuccessor(key)) {
                reachedDestination = true;
            }

            NodeInfo nextBestNode = currentNode.getNextBestNode(key);

            if (currentNode.getInfo().equals(nextBestNode))
                nextBestNode = currentNode.getSuccessor();

            System.out.format("Redirecting message to next best node, with ID %d\n", nextBestNode.getId());

            Mailman.sendOperation(nextBestNode, this);

        } catch (IOException e) {
            e.printStackTrace();
        }

        fingerTable.updateFingerTable(origin);
        fingerTable.updateFingerTable(senderNode);
    }
}
