package server;

import encryptUtils.KeyEncryption;
import server.exceptions.DecryptionFailedException;

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

    private final static String STORED_FILES_DIR = "StoredFiles/";
    private final static String KEYS_DIR = "Keys/";

    public FileManager(int nodeId) throws IOException, NoSuchAlgorithmException {
        BASE_DIR = String.valueOf(nodeId) + "/";
        createDirectories();
        KeyEncryption.initializeKeys(getKeysDir());
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

    private String getKeysDir() {
        return BASE_DIR + KEYS_DIR;
    }

    public void storeFile(BigInteger key, byte[] content) throws IOException {
        createDirectories();

        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(content);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public byte[] loadFile(String path) throws IOException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(path);

        byte[] content = new byte[fileInputStream.available()];
        fileInputStream.read(content);
        return KeyEncryption.encrypt(content);
    }

    public void saveRestoredFile(String path, byte[] content) throws IOException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, ClassNotFoundException, DecryptionFailedException {
        byte[] decryptedContent = KeyEncryption.decrypt(content);

        FileOutputStream fileOutputStream = new FileOutputStream(path);
        fileOutputStream.write(decryptedContent);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public void delete(BigInteger key) {
        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        file.delete();
    }

}
