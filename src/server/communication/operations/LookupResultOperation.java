package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;

public class LookupResultOperation extends Operation {
    private final NodeInfo resultNode;
    private final BigInteger key;

    public LookupResultOperation(NodeInfo origin, NodeInfo resultNode, BigInteger key) {
        super(origin);
        this.resultNode = resultNode;
        this.key = key;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Lookup operation for key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " resolved as " + resultNode.getId());
        currentNode.onLookupFinished(key, resultNode);
    }

}
