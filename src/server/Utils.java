package server;

import javax.xml.bind.DatatypeConverter;
import javax.xml.crypto.Data;
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

    /**
     * Truncates a byte array to an integer starting at its end.
     *
     * @param array Array to truncante.
     * @return
     */
    public static int truncateToInt(byte[] array) {
        int id = 0;
        for (int i = 0; i < Integer.BYTES; i++)
            id = id | (array[array.length - i - 1] << (8 * i));

        return id;
    }
}
