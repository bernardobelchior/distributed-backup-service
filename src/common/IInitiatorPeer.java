package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IInitiatorPeer extends Remote {

    boolean backup(String pathName) throws RemoteException;

    boolean restore(String filename) throws RemoteException;

    boolean delete(String filename) throws RemoteException;

    String state() throws RemoteException;
}
