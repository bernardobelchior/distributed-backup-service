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
    static final int OPERATION_TIMEOUT = 30; //In seconds
    public static final int MAXIMUM_HOPS = 8;
    private final Node node;
    private final ConcurrentHashMap<BigInteger, byte[]> localValues = new ConcurrentHashMap<>();
    private final FileManager fileManager;

    DistributedHashTable(Node node) throws IOException, NoSuchAlgorithmException {

        this.node = node;
        this.fileManager = new FileManager(node.getInfo().getId());
    }

    /**
     * Starts the process of insertion of the this value with the this key from this node.
     *
     * @param key
     * @param value
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */

    public boolean insert(BigInteger key, byte[] value) throws InterruptedException, ExecutionException, TimeoutException {
        return node.insert(key, value).get(OPERATION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Starts the process of getting the value with the this key to this node.
     *
     * @param key
     * @return
     */
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

    /**
     * Starts the process of deletion of the values in the other nodes with the key.
     *
     * @param key
     * @return
     */
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


    /**
     * It stores locally and in the "LocalValues" Concurrent Hash Map the value with the given key.
     *
     * @param key
     * @param value
     * @return
     */
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

    /**
     * It deletes locally and in the "LocalValues" Concurrent Hash Map the value with the given key.
     * @param key
     * @return
     */
    boolean deleteKey(BigInteger key) {
        localValues.remove(key);
        fileManager.delete(key);

        return true;
    }

    /**
     * Getting the current state of the node.
     *
     * @return
     */
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

    /**
     *It gets the keys and values that are store locally and belong to the given node.
     *
     * @param node
     * @return
     */
    ConcurrentHashMap<BigInteger, byte[]> getKeysBelongingTo(NodeInfo node) {
        ConcurrentHashMap<BigInteger, byte[]> predecessorKeys = new ConcurrentHashMap<>();
        localValues.forEach((key, value) -> {
            if (!between(node, this.node.getInfo(), key))
                predecessorKeys.put(key, value);
        });

        return predecessorKeys;
    }


    /** It gets the fileManager.
     *
     * @return
     */
    public FileManager getFileManager() {
        return fileManager;
    }


    /**
     * It stores locally and in the "localValues" Concurrent Hash Map the given keys and values.
     * @param keys
     */
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

    /**
     * It gets the value stored locally corresponding to the given key.
     * @param key
     * @return
     */
    byte[] getLocalValue(BigInteger key) {
        return localValues.get(key);
    }

    /**
     * It gets the Key set.
     *
     * @return
     */
    HashSet<BigInteger> getKeySet() {
        return new HashSet<>(Collections.list(localValues.keys()));
    }

    /**
     * It gets the "localValues" Concurrent Hash Map.
     *
     * @return
     */
    ConcurrentHashMap<BigInteger, byte[]> getLocalValues() {
        return localValues;
    }

    /**
     * It gets the differences bewteen the locally stored keys and the given keys.
     *
     * @param keys
     * @return
     */
    ConcurrentHashMap<BigInteger, byte[]> getDifference(HashSet<BigInteger> keys) {
        ConcurrentHashMap<BigInteger, byte[]> difference = new ConcurrentHashMap<>();
        localValues.forEach((key, value) -> {
            if (keys.contains(key))
                difference.put(key, value);
        });
        return difference;
    }
}
