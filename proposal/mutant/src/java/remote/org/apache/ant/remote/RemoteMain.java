package org.apache.ant.remote;

import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

/**
 * Command line to run Ant core from a remote server
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 27 January 2002
 */
public class RemoteMain {
    /**
     * The main program for the RemoteLauncher class
     *
     * @param args The command line arguments
     * @exception Exception if the launcher encounters a problem
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            throw new Exception("You must specify the location of the " 
                + "remote server");
        }
        
        String antHome = args[0];

        URL[] remoteStart = new URL[1];
        remoteStart[0] = new URL(antHome + "/lib/start.jar");
        URLClassLoader remoteLoader = new URLClassLoader(remoteStart);

        String[] realArgs = new String[args.length - 1];
        System.arraycopy(args, 1, realArgs, 0, realArgs.length);

        System.out.print("Loading remote Ant ... ");
        Class launcher 
            = Class.forName("org.apache.ant.start.Main", true, remoteLoader);

        final Class[] param = {Class.forName("[Ljava.lang.String;")};
        final Method startMethod = launcher.getMethod("main", param);
        final Object[] arguments = {realArgs};
        System.out.println("Done");
        System.out.println("Starting Ant from remote server");
        startMethod.invoke(null, arguments);
    }
}

