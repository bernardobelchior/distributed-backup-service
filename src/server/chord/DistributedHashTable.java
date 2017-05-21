package server.chord;

import server.FileManager;

import javax.xml.bind.DatatypeConverter;
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

    public boolean put(BigInteger key, byte[] value) {
        CompletableFuture<Boolean> put = node.put(key, value);

        try {
            return put.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Put operation for key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " timed out. Please try again.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] get(BigInteger key) {
        CompletableFuture<byte[]> get = node.get(key);

        try {
            byte[] value = get.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
            fileManager.saveRestoredFile(key, value);
            return value;
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Get operation for key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " timed out. Please try again.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean remove(BigInteger key) {
        CompletableFuture<Boolean> remove = node.remove(key);

        try {
            return remove.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Remove operation for key " + DatatypeConverter.printHexBinary(key.toByteArray()) + "timed out. Please try again.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    boolean removeLocally(BigInteger key) {
        localValues.remove(key);
        fileManager.delete(key);

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

    void remappedKeys(ConcurrentHashMap<BigInteger, byte[]> keys) {
        localValues.putAll(keys);
    }

    public byte[] getLocalValue(BigInteger key) {
        return localValues.get(key);
    }
}
