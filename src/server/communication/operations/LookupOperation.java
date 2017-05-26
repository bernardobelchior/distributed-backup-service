package server.communication.operations;

import server.chord.FingerTable;
import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.communication.Operation;

import java.math.BigInteger;

import static server.chord.DistributedHashTable.MAXIMUM_HOPS;

public class LookupOperation extends Operation {
    private BigInteger key;
    private NodeInfo lastNode;
    private boolean reachedDestination = false;
    private int timeToLive;

    public LookupOperation(FingerTable fingerTable, NodeInfo origin, BigInteger key, NodeInfo targetNode) {
        super(origin);
        lastNode = origin;
        this.key = key;
        timeToLive = MAXIMUM_HOPS;

        if (fingerTable.keyBelongsToSuccessor(key) && fingerTable.getSuccessor().equals(targetNode))
            reachedDestination = true;
    }

    @Override
    public void run(Node currentNode) {
        if (--timeToLive < 0)
            return;

        NodeInfo lastNode = this.lastNode;
        this.lastNode = currentNode.getInfo();

        if (reachedDestination) {
            try {
                System.out.println(currentNode.getInfo());
                System.out.println(this.lastNode);
                Mailman.sendOperation(origin, new LookupResultOperation(origin, currentNode.getInfo(), key));
                currentNode.informAboutExistence(origin);
            } catch (Exception e) {
                currentNode.informAboutFailure(origin);
            } finally {
                currentNode.informAboutExistence(lastNode);
            }

            return;
        }

        if (currentNode.keyBelongsToSuccessor(key))
            reachedDestination = true;

        NodeInfo nextBestNode = currentNode.getNextBestNode(key);

        if (currentNode.getInfo().equals(nextBestNode))
            nextBestNode = currentNode.getSuccessor();

        try {
            Mailman.sendOperation(nextBestNode, this);
            currentNode.informAboutExistence(origin);
        } catch (Exception e) {
            System.out.format("Failure of node with ID %d\n", nextBestNode.getId());
            currentNode.informAboutFailure(nextBestNode);
        } finally {
            currentNode.informAboutExistence(lastNode);
        }
    }
}
