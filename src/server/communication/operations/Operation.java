package server.communication.operations;

import server.chord.Node;

import java.io.Serializable;

public interface Operation<T> extends Serializable {
    void run(Node<T> currentNode);
}
