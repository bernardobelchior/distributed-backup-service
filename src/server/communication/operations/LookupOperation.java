package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

import java.io.IOException;
import java.math.BigInteger;

import static server.chord.DistributedHashTable.MAXIMUM_HOPS;

public class LookupOperation extends Operation {
    private BigInteger key;
    private NodeInfo lastNode;
    private boolean reachedDestination = false;
    private int timeToLive;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        super(origin);
        lastNode = origin;
        this.key = key;
        timeToLive = MAXIMUM_HOPS;
    }

    @Override
    public void run(Node currentNode) {
        if (--timeToLive < 0)
            return;

        NodeInfo senderNode = lastNode;
        lastNode = currentNode.getInfo();

        if (reachedDestination || !currentNode.hasSuccessors()) {

            try {
                Mailman.sendOperation(origin, new LookupResultOperation(origin, currentNode.getInfo(), key));
                currentNode.informAboutExistence(origin);
            } catch (IOException e) {
                currentNode.informAboutFailure(origin);
            } finally {
                currentNode.informAboutExistence(senderNode);
            }

            return;
        }

        if (currentNode.keyBelongsToSuccessor(key))
            reachedDestination = true;

        NodeInfo nextBestNode = currentNode.getNextBestNode(key);

        if (nextBestNode == null || currentNode.getInfo().equals(nextBestNode))
            nextBestNode = currentNode.getSuccessor();

        try {
            Mailman.sendOperation(nextBestNode, this);
            currentNode.informAboutExistence(origin);
        } catch (IOException e) {
            System.out.format("Failure of node with ID %d\n", nextBestNode.getId());
            currentNode.informAboutFailure(nextBestNode);
        } finally {
            currentNode.informAboutExistence(senderNode);
        }


    }
}
