/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Runs an external program.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class Execute {

    /** Invalid exit code. **/
    public final static int INVALID = Integer.MAX_VALUE;

    private String[] cmdl = null;
    private String[] env = null;
    private int exitValue = INVALID;
    private ExecuteStreamHandler streamHandler;
    private ExecuteWatchdog watchdog;
    private File workingDirectory = null;
    private String antRun;

    private static String antWorkingDirectory = System.getProperty("user.dir");
    private static String myos = System.getProperty("os.name");

    private static Method execWithCWD = null;
    static {
	try {
	    // JDK 1.3 API extension:
	    // Runtime.exec(String[] cmdarray, String[] envp, File dir)
	    execWithCWD = Runtime.class.getMethod("exec", new Class[] {String[].class, String[].class, File.class});
	} catch (NoSuchMethodException nsme) {
	    // OK.
	}
    }

    /**
     * Creates a new execute object using <code>PumpStreamHandler</code> for
     * stream handling.
     */
    public Execute() {
        this(new PumpStreamHandler(), null);
    }


    /**
     * Creates a new execute object.
     *
     * @param streamHandler the stream handler used to handle the input and
     *        output streams of the subprocess.
     */
    public Execute(ExecuteStreamHandler streamHandler) {
        this(streamHandler, null);
    }

    /**
     * Creates a new execute object.
     *
     * @param streamHandler the stream handler used to handle the input and
     *        output streams of the subprocess.
     * @param watchdog a watchdog for the subprocess or <code>null</code> to
     *        to disable a timeout for the subprocess.
     */
    public Execute(ExecuteStreamHandler streamHandler, ExecuteWatchdog watchdog) {
        this.streamHandler = streamHandler;
        this.watchdog = watchdog;
    }


    /**
     * Returns the commandline used to create a subprocess.
     *
     * @return the commandline used to create a subprocess
     */
    public String[] getCommandline() {
        return cmdl;
    }


    /**
     * Sets the commandline of the subprocess to launch.
     *
     * @param commandline the commandline of the subprocess to launch
     */
    public void setCommandline(String[] commandline) {
        cmdl = commandline;
    }

    /**
     * Returns the commandline used to create a subprocess.
     *
     * @return the commandline used to create a subprocess
     */
    public String[] getEnvironment() {
        return env;
    }


    /**
     * Sets the environment variables for the subprocess to launch.
     *
     * @param commandline array of Strings, each element of which has
     * an environment variable settings in format <em>key=value</em> 
     */
    public void setEnvironment(String[] env) {
        this.env = env;
    }

    /**
     * Sets the working directory of the process to execute.
     *
     * <p>This is emulated using the antRun scripts unless the OS is
     * Windows NT in which case a cmd.exe is spawned,
     * or MRJ and setting user.dir works, or JDK 1.3 and there is
     * official support in java.lang.Runtime.
     *
     * @param wd the working directory of the process.
     */
    public void setWorkingDirectory(File wd) {
	if (wd == null || wd.getAbsolutePath().equals(antWorkingDirectory))
	    workingDirectory = null;
	else
	    workingDirectory = wd;
    }

    /**
     * Set the name of the antRun script using the project's value.
     *
     * @param project the current project.
     */
    public void setAntRun(Project project) throws BuildException {
    	if (myos.equals("Mac OS") || execWithCWD != null)
            return;

        String ant = project.getProperty("ant.home");
        if (ant == null) {
            throw new BuildException("Property 'ant.home' not found");
        }

        if (myos.toLowerCase().indexOf("windows") >= 0) {
            antRun = project.resolveFile(ant + "/bin/antRun.bat").toString();
        } else {
            antRun = project.resolveFile(ant + "/bin/antRun").toString();
        }
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>
     * @exception java.io.IOExcpetion The exception is thrown, if launching
     *            of the subprocess failed
     */
    public int execute() throws IOException {
        final Process process = exec();
        try {
            streamHandler.setProcessInputStream(process.getOutputStream());
            streamHandler.setProcessOutputStream(process.getInputStream());
            streamHandler.setProcessErrorStream(process.getErrorStream());
        } catch (IOException e) {
            process.destroy();
            throw e;
        }
        streamHandler.start();
        if (watchdog != null) watchdog.start(process);
        waitFor(process);
        if (watchdog != null) watchdog.stop();
        streamHandler.stop();
        if (watchdog != null) watchdog.checkException();
        return getExitValue();
    }


    protected Process exec() throws IOException {
	if (workingDirectory == null) {
	    // Easy.
	    return Runtime.getRuntime().exec(cmdl, getEnvironment());
	} else if (execWithCWD != null) {
	    // The best way to set cwd, if you have JDK 1.3.
	    try {
		Object[] arguments = new Object[] {getCommandline(), getEnvironment(), workingDirectory};
		return (Process)execWithCWD.invoke(Runtime.getRuntime(), arguments);
            } catch (InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if (t instanceof ThreadDeath) {
                    throw (ThreadDeath)t;
                } else if (t instanceof IOException) {
                    throw (IOException)t;
                } else {
                    throw new IOException(t.toString());
                }
	    } catch (Exception e) {
		// IllegalAccess, IllegalArgument, ClassCast
		throw new IOException(e.toString());
	    }
	} else if (myos.equals("Mac OS")) {
	    // Dubious Mac hack.
	    System.getProperties().put("user.dir", 
				       workingDirectory.getAbsolutePath());
	    try {
		return Runtime.getRuntime().exec(cmdl, getEnvironment());
	    } finally {
                System.getProperties().put("user.dir", antWorkingDirectory);
	    }
	} else if (myos.toLowerCase().indexOf("windows") >= 0 &&
		   (myos.toLowerCase().indexOf("nt") >= 0 ||
		    myos.indexOf("2000") >= 0)) {
	    // cmd /c cd works OK on Windows NT & friends.
	    String[] commandLine = new String[cmdl.length+5];
	    commandLine[0] = "cmd";
	    commandLine[1] = "/c";
	    commandLine[2] = "cd";
	    commandLine[3] = workingDirectory.getAbsolutePath();
	    commandLine[4] = "&&";
	    System.arraycopy(cmdl, 0, commandLine, 5, cmdl.length);
	    return Runtime.getRuntime().exec(commandLine, getEnvironment());
	} else {
	    // Fallback to the antRun wrapper script (POSIX, Win95/98, etc.):
	    String[] commandLine = new String[cmdl.length+2];
	    commandLine[0] = antRun;
	    commandLine[1] = workingDirectory.getAbsolutePath();
	    System.arraycopy(cmdl, 0, commandLine, 2, cmdl.length);
	    return Runtime.getRuntime().exec(commandLine, getEnvironment());
	}
    }

    protected void waitFor(Process process) {
        try {
            process.waitFor();
            setExitValue(process.exitValue());
        } catch (InterruptedException e) {}
    }

    protected void setExitValue(int value) {
        exitValue = value;
    }

    protected int getExitValue() {
        return exitValue;
    }
}
