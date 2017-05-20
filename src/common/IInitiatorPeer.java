package common;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IInitiatorPeer extends Remote {

    boolean backup(String pathName) throws IOException, ClassNotFoundException;

    boolean restore(String filename) throws RemoteException;

    boolean delete(String filename) throws RemoteException;

    String state() throws RemoteException;
}
