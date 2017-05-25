package server.communication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class OperationManager<T, U> {
    private final ConcurrentHashMap<T, CompletableFuture<U>> ongoingOperation = new ConcurrentHashMap<>();

    public CompletableFuture<U> get(T key) {
        return ongoingOperation.get(key);
    }

    public CompletableFuture<U> putIfAbsent(T key) {
        CompletableFuture<U> operation = ongoingOperation.putIfAbsent(key, new CompletableFuture<>());

        /*
         * Needed because putIfAbsent returns the LAST value associated with the key
         * If the key-value pair is a new one, this will return null
         */
        if(operation == null)
            operation = ongoingOperation.get(key);
        return operation;
    }

    public void operationFinished(T key, U value) {
        ongoingOperation.remove(key).complete(value);
    }

    public void operationFailed(T key, Exception exception) {
        ongoingOperation.remove(key).completeExceptionally(exception);
    }
}
