package common;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IInitiatorPeer extends Remote {

    boolean backup(String pathName) throws IOException, ClassNotFoundException;

    boolean restore(String filename) throws IOException;

    boolean delete(String filename) throws IOException;

    String state() throws RemoteException;
}
