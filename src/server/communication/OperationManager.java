package server.communication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class OperationManager<T, U> {
    private final ConcurrentHashMap<T, CompletableFuture<U>> ongoingOperation = new ConcurrentHashMap<>();

    public CompletableFuture<U> get(T key) {
        return ongoingOperation.get(key);
    }

    public CompletableFuture<U> putIfAbsent(T key) {
        return ongoingOperation.putIfAbsent(key, new CompletableFuture<>());
    }

    public CompletableFuture<U> put(T key) {
        CompletableFuture<U> operation = new CompletableFuture<>();

        ongoingOperation.put(key, operation);
        System.out.println("Put: " + ongoingOperation.toString());
        return operation;
    }

    public void operationFinished(T key, U value) {
        System.out.println("Operation Finished: " + ongoingOperation.toString());
        CompletableFuture<U> a = ongoingOperation.remove(key);
        a.complete(value);
    }

    public void operationFailed(T key, Exception exception) {
        ongoingOperation.remove(key).completeExceptionally(exception);
    }
}
