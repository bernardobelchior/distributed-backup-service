package server;

import common.IInitiatorPeer;
import server.chord.DistributedHashTable;
import server.exceptions.DecryptionFailedException;
import server.utils.Utils;

import javax.crypto.BadPaddingException;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;

public class InitiatorPeer extends UnicastRemoteObject implements IInitiatorPeer {
    private final DistributedHashTable dht;
    private final FileManager fileManager;

    InitiatorPeer(DistributedHashTable dht) throws IOException, NoSuchAlgorithmException {
        super();
        this.dht = dht;
        fileManager = dht.getFileManager();
    }

    @Override
    public boolean backup(String pathName) throws IOException {
        byte[] file;
        try {
            file = fileManager.loadFileFromDisk(pathName);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not encrypt file. Aborting backup...");
            return false;
        }

        BigInteger key;
        try {
            key = new BigInteger(Utils.hash(file));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        boolean ret = dht.insert(key, file);
        System.out.println("Filename " + pathName + " stored with key " + DatatypeConverter.printHexBinary(key.toByteArray()));
        return ret;
    }


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

    @Override
    public boolean delete(String hexKey) {
        BigInteger key = new BigInteger(DatatypeConverter.parseHexBinary(hexKey));
        boolean ret = dht.delete(key);
        System.out.println("File stored with key " + hexKey + " deleted successfully.");
        return ret;
    }

    @Override
    public String state() {
        return dht.getState();
    }
}
