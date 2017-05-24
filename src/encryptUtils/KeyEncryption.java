package encryptUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;

public class KeyEncryption {
    private static final String ALGORITHM = "RSA";
    private static final int KEY_LENGTH = 2048;

    private static final String PUBLIC_KEY_FILENAME = "public.key";
    private static final String PRIVATE_KEY_FILENAME = "private.key";

    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    public static void initializeKeys(String keyDir) throws NoSuchAlgorithmException, IOException {
        File pubKeyFile = new File(keyDir + PUBLIC_KEY_FILENAME);
        File privKeyFile = new File(keyDir + PRIVATE_KEY_FILENAME);

        try {
            publicKey = (PublicKey) loadKey(pubKeyFile);
            privateKey = (PrivateKey) loadKey(privKeyFile);
        } catch (Exception e) {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_LENGTH);

            final KeyPair kPair = keyGen.generateKeyPair();
            publicKey = kPair.getPublic();
            privateKey = kPair.getPrivate();

            saveKey(publicKey, pubKeyFile);
            saveKey(privateKey, privKeyFile);
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
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(content);
    }

    public static byte[] decrypt(byte[] content) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        System.out.println("Decrypting content with length :" + content.length);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(content);
    }

    private static Key loadKey(File path) throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path));
        return (Key) inputStream.readObject();
    }
}