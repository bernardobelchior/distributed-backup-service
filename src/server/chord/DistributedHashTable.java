package server.chord;

import server.FileManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static server.utils.Utils.between;

public class DistributedHashTable {
    private static final int OPERATION_TIMEOUT = 5; //In seconds
    public static final int MAXIMUM_HOPS = 8;
    private final Node node;
    private final ConcurrentHashMap<BigInteger, byte[]> localValues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<BigInteger, byte[]>> replicatedValues = new ConcurrentHashMap<>();
    private final FileManager fileManager;

    public DistributedHashTable(Node node) {
        this.node = node;
        this.fileManager = new FileManager(node.getInfo().getId());
    }

    public byte[] get(BigInteger key) {
        return null;
    }

    public boolean put(BigInteger key, byte[] value) {
        CompletableFuture<Boolean> put = node.put(key, value);

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

    public boolean remove(BigInteger key) {
        return false;
    }

    boolean backup(BigInteger key, byte[] value) {
        localValues.put(key, value);

        try {
            fileManager.saveFile(key, value);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Node ID: ");
        sb.append(node.getInfo().getId());
        sb.append("\n\n");

        sb.append(node.toString());
        return sb.toString();
    }

    void storeReplica(int nodeId, BigInteger key, byte[] value) {
        ConcurrentHashMap<BigInteger, byte[]> replicas = replicatedValues.getOrDefault(nodeId, new ConcurrentHashMap<>());
        replicas.put(key, value);
        replicatedValues.putIfAbsent(nodeId, replicas);
    }

    ConcurrentHashMap<BigInteger, byte[]> getNewPredecessorKeys(NodeInfo newPredecessor) {
        ConcurrentHashMap<BigInteger, byte[]> predecessorKeys = new ConcurrentHashMap<>();
        localValues.forEach((key, value) -> {
            if (!between(newPredecessor, node.getInfo(), key))
                predecessorKeys.put(key, value);
        });

        return predecessorKeys;
    }

    public void remappedKeys(ConcurrentHashMap<BigInteger, byte[]> keys) {
        localValues.putAll(keys);
    }
}
