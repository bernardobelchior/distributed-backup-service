package server.chord;

import server.FileManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DistributedHashTable<T> {
    private static final int OPERATION_TIMEOUT = 5; //In seconds
    private final Node<T> self;
    private final ConcurrentHashMap<BigInteger, T> localValues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BigInteger, T> replicatedValues = new ConcurrentHashMap<>();
    private final FileManager fileManager;

    public DistributedHashTable(Node<T> self) {
        this.self = self;
        this.fileManager = new FileManager(self.getInfo().getId());
    }

    public T get(Object key) {
        return null;
    }

    public boolean put(BigInteger key, T value) {
        CompletableFuture<Boolean> put = self.put(key, value);

        try {
            return put.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Operation timed out. Please try again.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public T remove(Object key) {
        return null;
    }

    boolean storeLocally(BigInteger key, T value) {
        localValues.put(key, value);

        try {
            fileManager.saveObject(key, value);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Node ID: ");
        sb.append(self.getInfo().getId());
        sb.append("\n\n");

        sb.append(self.getFingerTable().toString());
        return sb.toString();
    }

    void backup(BigInteger key, T value) {
        replicatedValues.put(key, value);
    }

    T getReplicated(BigInteger key) {
        return replicatedValues.get(key);
    }
}
