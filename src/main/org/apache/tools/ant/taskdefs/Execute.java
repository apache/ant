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
    private File workingDirectory;
    private String antRun;

    private static String antWorkingDirectory = 
        (new File((new File(".")).getAbsolutePath())).getParent();
    private static String myos = System.getProperty("os.name");

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
        String[] commandLine = cmdl;

        if (workingDirectory != null && 
            !antWorkingDirectory.equals(workingDirectory.getAbsolutePath())) {

            if (myos.toLowerCase().indexOf("windows") >= 0 &&
                (myos.toLowerCase().indexOf("nt") >= 0 ||
                 myos.indexOf("2000") >= 0)) {

                commandLine = new String[cmdl.length+5];
                commandLine[0] = "cmd";
                commandLine[1] = "/c";
                commandLine[2] = "cd";
                commandLine[3] = workingDirectory.getAbsolutePath();
                commandLine[4] = "&&";
                System.arraycopy(cmdl, 0, commandLine, 5, cmdl.length);

            } else {
                commandLine = new String[cmdl.length+2];
                commandLine[0] = antRun;
                commandLine[1] = workingDirectory.getAbsolutePath();
                System.arraycopy(cmdl, 0, commandLine, 2, cmdl.length);
            }
        }
        
        return commandLine;
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
     * Windows NT in which case a cmd.exe is spawned.  
     *
     * @param wd the working directory of the process.
     */
    public void setWorkingDirectory(File wd) {
        workingDirectory = wd;
    }

    /**
     * Set the name of the antRun script using the project's value.
     *
     * @param project the current project.
     */
    public void setAntRun(Project project) throws BuildException {
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
        return Runtime.getRuntime().exec(getCommandline(), getEnvironment());
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
