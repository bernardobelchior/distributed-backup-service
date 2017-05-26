package server.chord;

import server.FileManager;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;

import static server.utils.Utils.between;

public class DistributedHashTable {
    static final int OPERATION_TIMEOUT = 1; //In seconds
    public static final int MAXIMUM_HOPS = 8;
    private final Node node;
    private final ConcurrentHashMap<BigInteger, byte[]> localValues = new ConcurrentHashMap<>();
    private final FileManager fileManager;

    DistributedHashTable(Node node) throws IOException, NoSuchAlgorithmException {

        this.node = node;
        this.fileManager = new FileManager(node.getInfo().getId());
    }

    public boolean insert(BigInteger key, byte[] value) throws InterruptedException, ExecutionException, TimeoutException {
        return node.insert(key, value).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
    }

    public byte[] get(BigInteger key) {
        CompletableFuture<byte[]> get = node.get(key);

        try {
            return get.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Get operation for key " + DatatypeConverter.printHexBinary(key.toByteArray()) + " timed out. Please try again.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean delete(BigInteger key) {
        CompletableFuture<Boolean> delete = node.delete(key);

        try {
            return delete.get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.err.println("Delete operation for key " + DatatypeConverter.printHexBinary(key.toByteArray()) + "timed out. Please try again.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    boolean storeKey(BigInteger key, byte[] value) {
        localValues.put(key, value);

        try {
            fileManager.storeFile(key, value);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    boolean deleteKey(BigInteger key) {
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

        sb.append("\n\nKeys stored:\n");
        localValues.forEach((key, value) -> {
            sb.append(DatatypeConverter.printHexBinary(key.toByteArray()));
            sb.append("\n");
        });

        return sb.toString();
    }

    ConcurrentHashMap<BigInteger, byte[]> getKeysBelongingTo(NodeInfo node) {
        ConcurrentHashMap<BigInteger, byte[]> predecessorKeys = new ConcurrentHashMap<>();
        localValues.forEach((key, value) -> {
            if (!between(node, this.node.getInfo(), key))
                predecessorKeys.put(key, value);
        });

        return predecessorKeys;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    void storeKeys(ConcurrentHashMap<BigInteger, byte[]> keys) {
        localValues.putAll(keys);

        for (Map.Entry<BigInteger, byte[]> entry : keys.entrySet()) {
            try {
                fileManager.storeFile(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    byte[] getLocalValue(BigInteger key) {
        return localValues.get(key);
    }

    HashSet<BigInteger> getAllKeys() {
        return new HashSet<>(Collections.list(localValues.keys()));
    }

    ConcurrentHashMap<BigInteger, byte[]> getDifference(HashSet<BigInteger> keys) {
        ConcurrentHashMap<BigInteger, byte[]> difference = new ConcurrentHashMap<>();
        localValues.forEach((key, value) -> {
            if (keys.contains(key))
                difference.put(key, value);
        });
        return difference;
    }
}
