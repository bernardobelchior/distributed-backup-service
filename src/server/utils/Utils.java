package server.utils;

import server.chord.NodeInfo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static server.chord.Node.MAX_NODES;

public class Utils {
    private static final String HASH_FUNCTION = "SHA-1";

    public static byte[] hash(byte[] message) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(HASH_FUNCTION).digest(message);
    }

    public static int addToNodeId(int nodeId, int value) {
        return Integer.remainderUnsigned(nodeId + value, MAX_NODES);
    }

    public static int getNodeFromKey(BigInteger key) {
        return Integer.remainderUnsigned(key.intValue(), MAX_NODES);
    }

    public static BigInteger getSuccessorKey(NodeInfo nodeInfo) {
        return BigInteger.valueOf(addToNodeId(nodeInfo.getId(), 1));
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public static boolean between(NodeInfo lower, NodeInfo upper, BigInteger key) {
        return between(lower.getId(), upper.getId(), key);
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public static boolean between(int lower, int upper, BigInteger key) {
        int keyOwner = Integer.remainderUnsigned(key.intValue(), MAX_NODES);

        if (lower < upper)
            return keyOwner > lower && keyOwner <= upper;
        else
            return keyOwner > lower || keyOwner <= upper;
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public static boolean between(NodeInfo lower, NodeInfo upper, int key) {
        return between(lower.getId(), upper.getId(), key);
    }

    /**
     * Check if a given key is between the lower and upper keys in the Chord circle
     *
     * @param lower
     * @param upper
     * @param key
     * @return true if the key is between the other two, or equal to the upper key
     */
    public static boolean between(int lower, int upper, int key) {

        if (lower < upper)
            return key > lower && key <= upper;
        else
            return key > lower || key <= upper;
    }

	/*public byte[] turnToByteArray(Object object){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] bArray = null;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(object);
			out.flush();
			byte[] bArray = bos.toByteArray();
			} finally {
				try {
					bos.close();
				} catch (IOException ex) {}
			}
		return bArray;		
	}*/
}
