package common;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IInitiatorPeer extends Remote {

    boolean backup(String pathName) throws IOException, RemoteException;

    boolean restore(String hexKey, String filename) throws IOException, RemoteException;

    boolean delete(String hexKey) throws RemoteException;

    String state() throws RemoteException;
}
