package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

import java.math.BigInteger;

public class LookupResultOperation extends Operation {
    private final NodeInfo resultNode;
    private final BigInteger key;

    public LookupResultOperation(NodeInfo origin, NodeInfo resultNode, BigInteger key) {
        super(origin);

        if (resultNode == null)
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }

        this.resultNode = resultNode;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        if (resultNode == null)
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
        currentNode.onLookupFinished(key, resultNode);
    }

}
