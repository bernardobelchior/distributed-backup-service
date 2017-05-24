package server.utils;

import server.exceptions.DecryptionFailedException;

import javax.crypto.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class Encryption {
    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 128;

    private static final String KEY_FILENAME = "secret.key";

    private static SecretKey secretKey;

    public static void initializeKeys(String keyDir) throws NoSuchAlgorithmException, IOException {
        File keyFile = new File(keyDir + KEY_FILENAME);

        try {
            secretKey = (SecretKey) loadKey(keyFile);
        } catch (Exception e) {
            final KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_LENGTH);
            secretKey = keyGen.generateKey();
            saveKey(secretKey, keyFile);
        }
    }

    private static void saveKey(Key key, File file) throws IOException {
        ObjectOutputStream keyOutputStream = new ObjectOutputStream(new FileOutputStream(file));
        keyOutputStream.writeObject(key);
        keyOutputStream.flush();
        keyOutputStream.close();
    }


    public static byte[] encrypt(byte[] content) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(content);
    }

    public static byte[] decrypt(byte[] content) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, DecryptionFailedException {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedContent = cipher.doFinal(content);

        if (decryptedContent == null)
            throw new DecryptionFailedException();

        return decryptedContent;
    }

    private static Key loadKey(File path) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path));
        return (Key) inputStream.readObject();
    }
}