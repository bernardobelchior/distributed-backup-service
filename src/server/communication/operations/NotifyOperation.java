package server.communication.operations;

import server.chord.Node;
import server.chord.NodeInfo;

/**
 * Created by epassos on 5/16/17.
 */
public class NotifyOperation implements Operation{

    NodeInfo origin;

    public NotifyOperation(NodeInfo origin){
        this.origin = origin;
    }

    @Override
    public void run(Node currentNode) {
        System.out.println("Received Notification");
        if(currentNode.getFingerTable().updatePredecessor(origin))
            System.out.println("Notified of new predecessor.\nSetting predecessor to " + origin.getId());
    }

    @Override
    public String getKey() {
        return null;
    }
}
