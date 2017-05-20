package server.chord;

import server.FileManager;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DistributedHashTable {
    private static final int OPERATION_TIMEOUT = 5; //In seconds
    public static final int MAXIMUM_HOPS = 8;
    private final Node self;
    private final ConcurrentHashMap<BigInteger, byte[]> localValues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BigInteger, byte[]> replicatedValues = new ConcurrentHashMap<>();
    private final FileManager fileManager;

    public DistributedHashTable(Node self) throws IOException, NoSuchAlgorithmException {
        this.self = self;
        this.fileManager = new FileManager(self.getInfo().getId());
    }

    public byte[] get(Object key) {
        return null;
    }

    public boolean put(BigInteger key, byte[] value) {
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

    public boolean remove(Object key) {
        return false;
    }

    boolean storeLocally(BigInteger key, byte[] value) throws ClassNotFoundException {
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
        sb.append(self.getInfo().getId());
        sb.append("\n\n");

        sb.append(self.getFingerTable().toString());
        return sb.toString();
    }

    void backup(BigInteger key, byte[] value) {
        replicatedValues.put(key, value);
    }

    byte[] getReplicated(BigInteger key) {
        return replicatedValues.get(key);
    }
}
