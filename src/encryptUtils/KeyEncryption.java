package encryptUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

public class KeyEncryption{
	private static final String ALGORITHM = "RSA";
	private static final int KEYLENGTH = 2048;
	
	public static void genkeys(String publicKeyDir, String privateKeyDir){
		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
		keyGen.initialize(KEYLENGTH);
		
		final KeyPair kPair = keyGen.generateKeyPair();
		
		File pubKeyFile = new File(publicKeyDir);
		
		File privKeyFile = new File(privateKeyDir);
		
		if(!pubKeyFile.exists()){
		pubKeyFile.createNewFile();
		ObjectOutputStream pubKeyOutputStream = new ObjectOutPutStream(new FileOutputStream(pubKeyFile));
		pubKeyOutputStream.writeObject(kPair.getPublic());
		pubKeyOutputStream.close;}
		
		if(!privKeyFile.exists()){
		privKeyFile.createNewFile();
		ObjectOutputStream privKeyOutputStream = new ObjectOutPutStream(new FileOutputStream(privKeyFile));
		privKeyOutputStream.writeObject(kPair.getPrivate());
		privKeyOutputStream.close;}
		
	}


	public static byte[] encryptMessage(String text, PrivateKey privKey){
		byte[] cipherText = null;
		try{
		final Cipher cipher  = Cipher.getInstance(ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, privKey);
		cipherText = cipher.doFinal(text.getytes()))
		} catch(Exception e){
			e.printStackTrace();
		}
		return cipherText;
	}
	
	public static String decrypt(byte[] text, PrivateKey key) {
		    byte[] dectyptedText = null;
		    try {
		       final Cipher cipher = Cipher.getInstance(ALGORITHM);

		       cipher.init(Cipher.DECRYPT_MODE, key);
		      dectyptedText = cipher.doFinal(text);

		    } catch (Exception ex) {
		      ex.printStackTrace();
		    }

		    return new String(dectyptedText);
		  }
	
	
}