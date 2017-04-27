package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private static final String HASH_FUNCTION = "SHA-1";

    /**
     * Key length in bits.
     */
    public static final int KEY_LENGTH = 160;

    public static byte[] hash(byte[] message) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(HASH_FUNCTION).digest(message);
    }
}
