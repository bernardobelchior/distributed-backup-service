package common;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IInitiatorPeer extends Remote {
    boolean backup(String pathName) throws IOException;

    boolean restore(String hexKey, String filename) throws IOException;

    boolean delete(String hexKey) throws RemoteException;

    String state() throws RemoteException;
}
