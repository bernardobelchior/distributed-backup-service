package server;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import encryptUtils.KeyEncryption.genkeys;
import encryptUtils.KeyEncryption.encryptMessage;
import encryptUtils.KeyEncryption.decrypt;
import encryptUtils.KeyEncryption.obtainPublicKey;
import encryptUtils.KeyEncryption.obtainPrivateKey;

import java.security.PrivateKey;
import java.security.PublicKey;


public class FileManager {
    private final static String STORED_FILES_DIR = "StoredFiles/";
	//Não sei se é assim - Retirar se necessário
	private final static String PUBLIC_KEYS_PATH = "Keys/public.key"
	private final static String PRIVATE_KEYS_PATH = "Keys/private.key"
    private final String BASE_DIR;

    public FileManager(int nodeId) {
        BASE_DIR = String.valueOf(nodeId);
		genkeys(PUBLIC_KEYS_PATH,PRIVATE_KEYS_PATH);
    }

    private void createDirectories() {
        File parentDir = new File(BASE_DIR);
        parentDir.mkdir();

        File storedFiles = new File(getStoredFilesDir());
        storedFiles.mkdir();
    }

    private String getStoredFilesDir() {
        return BASE_DIR + "/" + STORED_FILES_DIR;
    }

    public void saveFile(BigInteger key, byte[] content) throws IOException {
		File privateKeyFile = new File(privateKeyFile);
		PrivateKey privKey = obtainPrivateKey(PRIVATE_KEYS_PATH);
		byte[] finalContent = null;
		finalContent = decrypt(content, privKey);
		
		
		
        createDirectories();
        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        fileOutputStream.write(finalContent);
        fileOutputStream.flush();
        fileOutputStream.close();
    }
}
