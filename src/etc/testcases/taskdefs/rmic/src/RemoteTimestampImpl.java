import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * this is the implementation
 */
public class RemoteTimestampImpl implements RemoteTimestamp {

	public long when() throws RemoteException {
		return System.currentTimeMillis();
	}
}
