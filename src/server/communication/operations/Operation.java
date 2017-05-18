package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

import java.io.Serializable;

public abstract class Operation implements Serializable {
    protected final NodeInfo origin;

    Operation(NodeInfo origin) {
        this.origin = origin;
    }

    public abstract void run(Node currentNode);

    public NodeInfo getOrigin() {
        return this.origin;
    }
}
