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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

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
    private Project project = null;
    private boolean newEnvironment = false;

    private static String antWorkingDirectory = System.getProperty("user.dir");
    private static CommandLauncher launcher = createCommandLauncher();
    private static Vector procEnvironment = null;

    /** 
     * Builds a command launcher for the OS and JVM we are running under
     */
    private static CommandLauncher createCommandLauncher()
    {
        // Try using a JDK 1.3 launcher
        try {
            return new Java13CommandLauncher();
        }
        catch ( NoSuchMethodException exc ) {
            // Ignore and keep try
        }

        String osname = System.getProperty("os.name").toLowerCase();
        if ( osname.indexOf("mac os") >= 0 ) {
            // Mac
            return new MacCommandLauncher(new CommandLauncher());
        }
        else if ( osname.indexOf("os/2") >= 0 ) {
            // OS/2 - use same mechanism as Windows 2000
            return new WinNTCommandLauncher(new CommandLauncher());
        }
        else if ( osname.indexOf("windows") >= 0 ) {
            // Windows.  Need to determine which JDK we're running in
            CommandLauncher baseLauncher;
            if ( System.getProperty("java.version").startsWith("1.1") ) {
                // JDK 1.1
                baseLauncher = new Java11CommandLauncher();
            }
            else {
                // JDK 1.2
                baseLauncher = new CommandLauncher();
            }

            // Determine if we're running under 2000/NT or 98/95
            if ( osname.indexOf("nt") >= 0 || osname.indexOf("2000") >= 0 ) {
                // Windows 2000/NT
                return new WinNTCommandLauncher(baseLauncher);
            }
            else {
                // Windows 98/95 - need to use an auxiliary script
                return new ScriptCommandLauncher("bin/antRun.bat", baseLauncher);
            }
        }
        else {
            // Generic
            return new ScriptCommandLauncher("bin/antRun", new CommandLauncher());
        }
    }

    /**
     * Find the list of environment variables for this process.
     */
    public static synchronized Vector getProcEnvironment() {
        if (procEnvironment != null) return procEnvironment;

        procEnvironment = new Vector();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Execute exe = new Execute(new PumpStreamHandler(out));
            exe.setCommandline(getProcEnvCommand());
            // Make sure we do not recurse forever
            exe.setNewenvironment(true);
            int retval = exe.execute();
            if ( retval != 0 ) {
                // Just try to use what we got
            }

            BufferedReader in = 
                new BufferedReader(new StringReader(out.toString()));
            String line;
            while ((line = in.readLine()) != null) {
                procEnvironment.addElement(line);
            }
        } 
        catch (java.io.IOException exc) {
            exc.printStackTrace();
            // Just try to see how much we got
        }
        return procEnvironment;
    }

    private static String[] getProcEnvCommand() {
        String osname = System.getProperty("os.name").toLowerCase();
        if ( osname.indexOf("mac os") >= 0 ) {
            // Mac
            // TODO: I have no idea how to get it, someone must fix it
            String[] cmd = null;
            return cmd;
        }
        else if ( osname.indexOf("os/2") >= 0 ) {
            // OS/2 - use same mechanism as Windows 2000
            // Not sure
            String[] cmd = {"cmd", "/c", "set" };
            return cmd;
        }
        else if ( osname.indexOf("indows") >= 0 ) {
            // Determine if we're running under 2000/NT or 98/95
            if ( osname.indexOf("nt") >= 0 || osname.indexOf("2000") >= 0 ) {
                // Windows 2000/NT
                String[] cmd = {"cmd", "/c", "set" };
                return cmd;
            }
            else {
                // Windows 98/95 - need to use an auxiliary script
                String[] cmd = {"command.com", "/c", "set" };
                return cmd;
            }
        }
        else {
            // Generic UNIX
            // Alternatively one could use: /bin/sh -c env
            String[] cmd = {"/usr/bin/env"};
            return cmd;
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
     * Set whether to propagate the default environment or not.
     *
     * @param newenv whether to propagate the process environment.
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Returns the environment used to create a subprocess.
     *
     * @return the environment used to create a subprocess
     */
    public String[] getEnvironment() {
        if (env == null || newEnvironment) return env;
        return patchEnvironment();
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
        this.project = project;
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>
     * @exception java.io.IOExcpetion The exception is thrown, if launching
     *            of the subprocess failed
     */
    public int execute() throws IOException {
        final Process process = launcher.exec(project, getCommandline(), getEnvironment(), workingDirectory);
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

    protected void waitFor(Process process) {
        try {
            process.waitFor();
            setExitValue(process.exitValue());
        } catch (InterruptedException e) {}
    }

    protected void setExitValue(int value) {
        exitValue = value;
    }

    public int getExitValue() {
        return exitValue;
    }

    /**
     * Patch the current environment with the new values from the user.
     * @return the patched environment
     */
    private String[] patchEnvironment() {
        Vector osEnv = (Vector) getProcEnvironment().clone();
        for (int i = 0; i < env.length; i++) {
            int pos = env[i].indexOf('=');
            // Get key including "="
            String key = env[i].substring(0, pos+1);
            int size = osEnv.size();
            for (int j = 0; j < size; j++) {
                if (((String)osEnv.elementAt(j)).startsWith(key)) {
                    osEnv.removeElementAt(j);
                    break;
                }
            }
            osEnv.addElement(env[i]);
        }
        String[] result = new String[osEnv.size()];
        osEnv.copyInto(result);
        return result;
    }

    /**
     * A utility method that runs an external command.  Writes the output and
     * error streams of the command to the project log.
     *
     * @param task      The task that the command is part of.  Used for logging
     * @param cmdline   The command to execute.
     *
     * @throws BuildException if the command does not return 0.
     */
    public static void runCommand(Task task, String[] cmdline) throws BuildException
    {
        try {
            task.log(Commandline.toString(cmdline), Project.MSG_VERBOSE);
            Execute exe = new Execute(new LogStreamHandler(task, 
                                                           Project.MSG_INFO,
                                                           Project.MSG_ERR));
            exe.setAntRun(task.getProject());
            exe.setCommandline(cmdline);
            int retval = exe.execute();
            if ( retval != 0 ) {
                throw new BuildException(cmdline[0] + " failed with return code " + retval, task.getLocation());
            }
        } 
        catch (java.io.IOException exc) {
            throw new BuildException("Could not launch " + cmdline[0] + ": " + exc, task.getLocation());
        }
    }

    /**
     * A command launcher for a particular JVM/OS platform.  This class is
     * a general purpose command launcher which can only launch commands in
     * the current working directory.
     */
    private static class CommandLauncher
    {
        /** 
         * Launches the given command in a new process.
         *
         * @param project       The project that the command is part of
         * @param cmd           The command to execute
         * @param env           The environment for the new process.  If null,
         *                      the environment of the current proccess is used.
         */
        public Process exec(Project project, String[] cmd, String[] env) throws IOException
        {
            if (project != null) {
                project.log("Execute:CommandLauncher: " +
                            Commandline.toString(cmd), Project.MSG_DEBUG);
            }                            
            return Runtime.getRuntime().exec(cmd, env);
        }

        /** 
         * Launches the given command in a new process, in the given working
         * directory.
         *
         * @param project       The project that the command is part of
         * @param cmd           The command to execute
         * @param env           The environment for the new process.  If null,
         *                      the environment of the current proccess is used.
         * @param workingDir    The directory to start the command in.  If null,
         *                      the current directory is used
         */
        public Process exec(Project project, String[] cmd, String[] env, File workingDir) throws IOException
        {
            if ( workingDir == null ) {
                return exec(project, cmd, env);
            }
            throw new IOException("Cannot execute a process in different directory under this JVM");
        }
    }

    /**
     * A command launcher for JDK/JRE 1.1 under Windows.  Fixes quoting problems
     * in Runtime.exec().  Can only launch commands in the current working
     * directory
     */
    private static class Java11CommandLauncher extends CommandLauncher
    {
        /**
         * Launches the given command in a new process.  Needs to quote
         * arguments
         */
        public Process exec(Project project, String[] cmd, String[] env) throws IOException 
        {
            // Need to quote arguments with spaces, and to escape quote characters
            String[] newcmd = new String[cmd.length];
            for ( int i = 0; i < cmd.length; i++ ) {
                newcmd[i] = Commandline.quoteArgument(cmd[i]);
            }
            if (project != null) {
                project.log("Execute:Java11CommandLauncher: " +
                            Commandline.toString(newcmd), Project.MSG_DEBUG);
            }                            
            return Runtime.getRuntime().exec(newcmd, env);
        }
    }

    /**
     * A command launcher for JDK/JRE 1.3 (and higher).  Uses the built-in
     * Runtime.exec() command
     */
    private static class Java13CommandLauncher extends CommandLauncher
    {
        public Java13CommandLauncher() throws NoSuchMethodException
        {
            // Locate method Runtime.exec(String[] cmdarray, String[] envp, File dir)
            _execWithCWD = Runtime.class.getMethod("exec", new Class[] {String[].class, String[].class, File.class});
        }

        /** 
         * Launches the given command in a new process, in the given working
         * directory
         */
        public Process exec(Project project, String[] cmd, String[] env, File workingDir) 
            throws IOException
        {
            try {
                if (project != null) {
                    project.log("Execute:Java13CommandLauncher: " +
                                Commandline.toString(cmd), Project.MSG_DEBUG);
                }                                
                Object[] arguments = { cmd, env, workingDir };
                return (Process)_execWithCWD.invoke(Runtime.getRuntime(), arguments);
            } 
            catch ( InvocationTargetException exc ) {
                Throwable realexc = exc.getTargetException();
                if ( realexc instanceof ThreadDeath ) {
                    throw (ThreadDeath)realexc;
                } 
                else if ( realexc instanceof IOException ) {
                    throw (IOException)realexc;
                } 
                else {
                    throw new IOException(realexc.getMessage());
                }
            } 
            catch ( Exception exc ) {
                // IllegalAccess, IllegalArgument, ClassCast
                throw new IOException(exc.getMessage());
            }
        }
        
        private Method _execWithCWD;
    }
    
    /**
     * A command launcher that proxies another command launcher.  
     *
     * Sub-classes override exec(args, env, workdir)
     */
    private static class CommandLauncherProxy extends CommandLauncher
    {
        CommandLauncherProxy(CommandLauncher launcher)
        {
            _launcher = launcher;
        }

        /** 
         * Launches the given command in a new process.  Delegates this
         * method to the proxied launcher
         */
        public Process exec(Project project, String[] cmd, String[] env) throws IOException
        {
            return _launcher.exec(project, cmd, env);
        }

        private CommandLauncher _launcher;
    }

    /**
     * A command launcher for Windows 2000/NT that uses 'cmd.exe' when
     * launching commands in directories other than the current working
     * directory.
     */
    private static class WinNTCommandLauncher extends CommandLauncherProxy
    {
        WinNTCommandLauncher(CommandLauncher launcher)
        {
            super(launcher);
        }

        /** 
         * Launches the given command in a new process, in the given working
         * directory.
         */
        public Process exec(Project project, String[] cmd, String[] env, File workingDir) throws IOException
        {
            if ( workingDir == null ) {
                return exec(project, cmd, env);
            }

            // Use cmd.exe to change to the specified directory before running
            // the command
            final int preCmdLength = 6;
            String[] newcmd = new String[cmd.length + preCmdLength];
            newcmd[0] = "cmd";
            newcmd[1] = "/c";
            newcmd[2] = "cd";
            newcmd[3] = "/d";
            newcmd[4] = workingDir.getAbsolutePath();
            newcmd[5] = "&&";
            System.arraycopy(cmd, 0, newcmd, preCmdLength, cmd.length);

            return exec(project, newcmd, env);
        }
    }

    /**
     * A command launcher for Mac that uses a dodgy mechanism to change
     * working directory before launching commands.
     */
    private static class MacCommandLauncher extends CommandLauncherProxy
    {
        MacCommandLauncher(CommandLauncher launcher)
        {
            super(launcher);
        }

        /** 
         * Launches the given command in a new process, in the given working
         * directory
         */
        public Process exec(Project project, String[] cmd, String[] env, File workingDir) throws IOException
        {
            if ( workingDir == null ) {
                return exec(project, cmd, env);
            }

            System.getProperties().put("user.dir", workingDir.getAbsolutePath());
            try {
                return exec(project, cmd, env);
            } 
            finally {
                System.getProperties().put("user.dir", antWorkingDirectory);
            }
        }
    }

    /**
     * A command launcher that uses an auxiliary script to launch commands
     * in directories other than the current working directory.
     */
    private static class ScriptCommandLauncher extends CommandLauncherProxy
    {
        ScriptCommandLauncher(String script, CommandLauncher launcher)
        {
            super(launcher);
            _script = script;
        }

        /** 
         * Launches the given command in a new process, in the given working
         * directory
         */
        public Process exec(Project project, String[] cmd, String[] env, File workingDir) throws IOException
        {
            if ( workingDir == null ) {
                return exec(project, cmd, env);
            }

            // Locate the auxiliary script
            if ( project == null ) {
                throw new IOException("Cannot locate antRun script: No project provided");
            }
            String antHome = project.getProperty("ant.home");
            if ( antHome == null ) {
                throw new IOException("Cannot locate antRun script: Property 'ant.home' not found");
            }
            String antRun = project.resolveFile(antHome + File.separator + _script).toString();

            // Build the command
            String[] newcmd = new String[cmd.length + 2];
            newcmd[0] = antRun;
            newcmd[1] = workingDir.getAbsolutePath();
            System.arraycopy(cmd, 0, newcmd, 2, cmd.length);
            
            return exec(project, newcmd, env);
        }

        private String _script;
    }
}
