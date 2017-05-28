package server;

import common.IInitiatorPeer;
import server.chord.DistributedHashTable;
import server.exceptions.DecryptionFailedException;
import server.utils.Encryption;
import server.utils.Utils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class InitiatorPeer extends UnicastRemoteObject implements IInitiatorPeer {
    private final DistributedHashTable dht;
    private final FileManager fileManager;

    InitiatorPeer(DistributedHashTable dht) throws IOException, NoSuchAlgorithmException {
        super();
        this.dht = dht;
        fileManager = dht.getFileManager();
    }

    /**
     * Starts the Backup Protocol from the file in the given path.
     * @param pathName
     * @return
     * @throws IOException
     */
    @Override
    public String backup(String pathName) throws IOException {
        byte[] file;
        try {
            file = fileManager.loadFile(pathName);
        } catch (IOException e) {
            return "Could not open file. Aborting backup...";
        }

        BigInteger key;
        try {
            key = new BigInteger(Utils.hash(file));
        } catch (NoSuchAlgorithmException e) {
            return "Could not create key for file backup. Aborting...";
        }

        try {
            file = Encryption.encrypt(file);
        } catch (InvalidKeyException | NoSuchPaddingException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException e) {
            return "Could not encrypt file. Aborting backup...";
        }

        try {
            if (dht.insert(key, file))
                return "File " + pathName + " stored with key " + DatatypeConverter.printHexBinary(key.toByteArray());
            else
                return "File " + pathName + " could not be inserted in the system.";

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return "Backup of file " + pathName + " timed out.";
        }
    }

    /**
     * Starts the Restore Protocol from the file with the given key and stores it in the given path.
     *
     * @param hexKey
     * @param filename
     * @return
     * @throws IOException
     */
    @Override
    public boolean restore(String hexKey, String filename) throws IOException {
        BigInteger key = new BigInteger(DatatypeConverter.parseHexBinary(hexKey));
        byte[] content = dht.get(key);

        if (content != null) {
            try {
                fileManager.saveRestoredFile(filename, content);
            } catch (DecryptionFailedException | BadPaddingException e) {
                System.err.println("Attempted decryption with wrong key. Restore failed...");
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.err.println("File stored with key " + hexKey + " not found.");
            return false;
        }

        System.out.println("File stored with key " + hexKey + " restored successfully.");
        return true;
    }

    /**
     *
     * Starts the Delete Protocol of the file with the given key.
     * @param hexKey
     * @return
     */
    @Override
    public boolean delete(String hexKey) {
        BigInteger key = new BigInteger(DatatypeConverter.parseHexBinary(hexKey));
        boolean ret = dht.delete(key);
        System.out.println("File stored with key " + hexKey + " deleted successfully.");
        return ret;
    }

    /**
     *
     * Starts the State protocol.
     *
     * @return
     */
    @Override
    public String state() {
        return dht.getState();
    }
}
