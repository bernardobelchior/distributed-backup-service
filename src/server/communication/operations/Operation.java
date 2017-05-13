package server.communication;

import server.chord.Node;

public interface Operation<T> {
    void run(Node currentNode);
}
