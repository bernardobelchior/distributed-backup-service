import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private static String hashFunction = "SHA-1";

    public static byte[] hash(byte[] message) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(hashFunction).digest(message);
    }
}
