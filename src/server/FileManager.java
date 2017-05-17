package server;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class FileManager {
    private final static String STORED_FILES_DIR = "StoredFiles/";
    private final String BASE_DIR;

    public FileManager(int nodeId) {
        BASE_DIR = String.valueOf(nodeId);
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
        createDirectories();
        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        fileOutputStream.write(content);
        fileOutputStream.flush();
        fileOutputStream.close();
    }
}
