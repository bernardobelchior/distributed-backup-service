package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;

public class SendKeysResultOperation extends Operation {
    public SendKeysResultOperation(NodeInfo origin) {
        super(origin);
    }

    /**
     * This Operation finishes the Send Keys Operation and removes it from the operation manager.
     *
     * @param currentNode
     */
    @Override
    public void run(Node currentNode) {
        currentNode.ongoingKeySendings.operationFinished(origin.getId(), true);
    }
}
