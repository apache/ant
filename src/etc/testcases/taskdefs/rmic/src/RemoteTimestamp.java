import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * this is the interface we remote
 */
public interface RemoteTimestamp extends Remote {
	long when() throws RemoteException ;
}

