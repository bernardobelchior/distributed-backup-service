package server;

import common.IInitiatorPeer;
import server.chord.DistributedHashTable;
import server.utils.Utils;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;

import encryptUtils.KeyEncryption;

import java.security.PrivateKey;
import java.security.PublicKey;

public class InitiatorPeer extends UnicastRemoteObject implements IInitiatorPeer {
    private final DistributedHashTable dht;
	
	
	private final static String PUBLIC_KEYS_PATH = "Keys/public.key";
	private final static String PRIVATE_KEYS_PATH = "Keys/private.key";

    InitiatorPeer(DistributedHashTable dht) throws IOException, NoSuchAlgorithmException {
        super();
        this.dht = dht;
		
		KeyEncryption.genkeys(PUBLIC_KEYS_PATH,PRIVATE_KEYS_PATH);
    }

    @Override
    public boolean backup(String pathName) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(pathName);
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
            return false;
        }

        byte[] file;
        try {
            file = new byte[fileInputStream.available()];
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            fileInputStream.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        File pubKeyPath = new File(PUBLIC_KEYS_PATH);
		PublicKey pubKey = KeyEncryption.obtainPublicKey(pubKeyPath);
		byte[] encryptedFile = null;
		encryptedFile = KeyEncryption.encrypt(file, pubKey);
		
        BigInteger key;
        try {
            key = new BigInteger(Utils.hash(file));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        boolean ret = dht.put(key, encryptedFile);
        System.out.println("Filename " + pathName + " stored with key " + DatatypeConverter.printHexBinary(key.toByteArray()));
        return ret;
    }

    @Override
    public boolean restore(String filename) {
        return false;
    }

    @Override
    public boolean delete(String filename) {
        return false;
    }

    @Override
    public String state() {
        return dht.getState();
    }
}
