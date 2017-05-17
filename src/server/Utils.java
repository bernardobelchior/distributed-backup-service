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
