package server;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class FileManager {
    private final static String STORED_FILES_DIR = "StoredFiles/";
    private static final String RESTORED_FILES_DIR = "RestoredFiles/";
    private final String BASE_DIR;

    public FileManager(int nodeId) {
        BASE_DIR = String.valueOf(nodeId);
    }

    private void createDirectories() {
        File parentDir = new File(BASE_DIR);
        parentDir.mkdir();

        File storedFiles = new File(getStoredFilesDir());
        storedFiles.mkdir();
        File restoredFiles = new File(getRestoredFilesDir());
        restoredFiles.mkdir();
    }

    private String getStoredFilesDir() {
        return BASE_DIR + "/" + STORED_FILES_DIR;
    }

    private String getRestoredFilesDir() {
        return BASE_DIR + "/" + RESTORED_FILES_DIR;
    }

    public void saveFile(BigInteger key, byte[] content) throws IOException {
        createDirectories();
        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        fileOutputStream.write(content);
        fileOutputStream.flush();
        fileOutputStream.close();
    }
    public void saveRestoredFile(BigInteger key, byte[] content) throws IOException {
        createDirectories();
        File file = new File(getRestoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        fileOutputStream.write(content);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public static byte[] loadFile(String pathName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(pathName);

        byte[] file = new byte[fileInputStream.available()];

        fileInputStream.read(file);

        return file;
    }

    public void delete(BigInteger key) {

        File file = new File(getStoredFilesDir() + DatatypeConverter.printHexBinary(key.toByteArray()));

        file.delete();
        /*
        Serve para eliminar pastas mais tarde pode ser util
         */
        /*File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    delete(f);
                } else {
                    f.delete();
                }
            }
        }*/

    }

}
