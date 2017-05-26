package server.communication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class OperationManager<T, U> {
    private final ConcurrentHashMap<T, CompletableFuture<U>> ongoingOperation = new ConcurrentHashMap<>();

    public CompletableFuture<U> get(T key) {
        return ongoingOperation.get(key);
    }

    public CompletableFuture<U> putIfAbsent(T key) {
        System.out.println("Put if absent: " + key);
        return ongoingOperation.putIfAbsent(key, new CompletableFuture<>());
    }

    public void operationFinished(T key, U value) {
        System.out.println(ongoingOperation);
        CompletableFuture<U> a = ongoingOperation.remove(key);
        System.out.println(ongoingOperation);

        if (a == null) {
            try {
                throw new Exception();
            } catch (Exception e) {
                System.out.println(a);
                e.printStackTrace();
            }
        }

        a.complete(value);
    }

    public void operationFailed(T key, Exception exception) {
        ongoingOperation.remove(key).completeExceptionally(exception);
    }
}
