package server.communication.operations;

import server.chord.Node;

import java.io.Serializable;

public interface Operation extends Serializable {
    void run(Node currentNode);
}
