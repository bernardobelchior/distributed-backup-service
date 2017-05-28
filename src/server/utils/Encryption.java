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

    /**
     * Generates the Encrypted key from the given key directory.
     *
     * @param keyDir
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static void initializeKey(String keyDir) throws NoSuchAlgorithmException, IOException {
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

    /**
     * Saves the given key in the given file.
     *
     * @param key
     * @param file
     * @throws IOException
     */

    private static void saveKey(Key key, File file) throws IOException {
        ObjectOutputStream keyOutputStream = new ObjectOutputStream(new FileOutputStream(file));
        keyOutputStream.writeObject(key);
        keyOutputStream.flush();
        keyOutputStream.close();
    }


    /**
     * Encrypts the given content.
     *
     * @param content
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static byte[] encrypt(byte[] content) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(content);
    }

    /**
     * Decrypts the given content.
     *
     * @param content
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws DecryptionFailedException
     */
    public static byte[] decrypt(byte[] content) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, DecryptionFailedException {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedContent = cipher.doFinal(content);

        if (decryptedContent == null)
            throw new DecryptionFailedException();

        return decryptedContent;
    }

    /**
     * Loads the key from the given path.
     *
     * @param path
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Key loadKey(File path) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path));
        return (Key) inputStream.readObject();
    }
}