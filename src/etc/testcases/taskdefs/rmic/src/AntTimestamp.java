import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Calendar;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DateUtils;


/**
 * This class imports a dependency on the Ant runtime classes,
 * so tests that classpath setup include them
 */
public class AntTimestamp implements RemoteTimestamp {


    /**
     * return the phase of the moon.
     * Note the completely different semantics of the other implementation,
     * which goes to show why signature is an inadeuqate way of verifying
     * how well an interface is implemented.
     *
     * @return
     * @throws RemoteException
     */
	public long when() throws RemoteException {
	    Calendar cal=Calendar.getInstance();
		return DateUtils.getPhaseOfMoon(cal);
	}
}
