package server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static server.chord.Node.MAX_NODES;

public class Utils {
    private static final String HASH_FUNCTION = "SHA-1";

    /**
     * Key length in bits.
     */
    public static final int KEY_LENGTH = 160;

    public static byte[] hash(byte[] message) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(HASH_FUNCTION).digest(message);
    }

    public static int addToNodeId(int nodeId, int value) {
        return Integer.remainderUnsigned(nodeId + value, MAX_NODES);
    }
}
