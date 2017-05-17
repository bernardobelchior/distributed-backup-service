package server;

import common.IInitiatorPeer;
import server.chord.DistributedHashTable;
import server.utils.Utils;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;

public class InitiatorPeer extends UnicastRemoteObject implements IInitiatorPeer {
    private final DistributedHashTable dht;

    InitiatorPeer(DistributedHashTable dht) throws RemoteException {
        super();
        this.dht = dht;
    }

    @Override
    public boolean backup(String pathName) {
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

        BigInteger key;
        try {
            key = new BigInteger(Utils.hash(file));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        boolean ret = dht.put(key, file);
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
