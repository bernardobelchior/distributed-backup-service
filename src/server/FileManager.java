package server;

import server.exceptions.DecryptionFailedException;
import server.utils.Encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class FileManager {
    private final String BASE_DIR;

    private static final String REPLICAS_DIR = "Replicas/";
    private static final String STORED_FILES_DIR = "StoredFiles/";
    private static final String KEYS_DIR = "Keys/";

    public FileManager(int nodeId) throws IOException, NoSuchAlgorithmException {
        BASE_DIR = String.valueOf(nodeId) + "/";
        createDirectories();
        Encryption.initializeKeys(getKeysDir());
    }

    private void createDirectories() {
        File parentDir = new File(BASE_DIR);
        parentDir.mkdir();

        File storedFilesDir = new File(getStoredFilesDir());
        storedFilesDir.mkdir();

        File keysDir = new File(getKeysDir());
        keysDir.mkdir();
    }

    private String getStoredFilesDir() {
        return BASE_DIR + STORED_FILES_DIR;
    }

    private String getReplicasDir() {
        return BASE_DIR + REPLICAS_DIR;
    }

    private String getKeysDir() {
        return BASE_DIR + KEYS_DIR;
    }

    public void storeReplica(BigInteger key, byte[] content) throws IOException {
        saveFile(getReplicasDir() + DatatypeConverter.printHexBinary(key.toByteArray()), content);
    }

    public void storeFile(BigInteger key, byte[] content) throws IOException {
        saveFile(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()), content);
    }

    private void saveFile(String path, byte[] content) throws IOException {
        createDirectories();
        File file = new File(path);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(content);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public byte[] loadFileFromDisk(String path) throws BadPaddingException, NoSuchAlgorithmException, IOException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        return Encryption.encrypt(loadFile(path));
    }

    private byte[] loadFile(String path) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(path);

        byte[] content = new byte[fileInputStream.available()];
        fileInputStream.read(content);
        return content;
    }

    public byte[] loadStoredFile(BigInteger key) throws IOException {
        return loadFile(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
    }

    public void saveRestoredFile(String path, byte[] content) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, ClassNotFoundException, DecryptionFailedException {
        saveFile(path, Encryption.decrypt(content));
    }

    public void delete(BigInteger key) {
        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        file.delete();
    }

}
