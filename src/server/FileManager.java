package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

public class FileManager {
    private final static String STORED_FILES_DIR = "StoredFiles/";
    private final String BASE_DIR;

    public FileManager(int nodeId) {
        BASE_DIR = String.valueOf(nodeId);
        createDirectories();
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

    public void saveObject(BigInteger key, Object object) throws IOException {
        File file = new File(getStoredFilesDir() + key.toString());
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));

        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
    }
}
