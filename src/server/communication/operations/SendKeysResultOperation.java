package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

public class SendKeysResultOperation extends Operation {
    public SendKeysResultOperation(NodeInfo origin) {
        super(origin);
    }

    @Override
    public void run(Node currentNode) {
        currentNode.ongoingKeySendings.operationFinished(origin.getId(), true);
    }
}
