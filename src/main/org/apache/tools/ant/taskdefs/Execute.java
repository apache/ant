/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;

/**
 * Runs an external program.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:jtulley@novell.com">Jeff Tulley</a>
 *
 * @since Ant 1.2
 *
 * @version $Revision$
 */
public class Execute {

    /** Invalid exit code. **/
    public static final int INVALID = Integer.MAX_VALUE;

    private String[] cmdl = null;
    private String[] env = null;
    private int exitValue = INVALID;
    private ExecuteStreamHandler streamHandler;
    private ExecuteWatchdog watchdog;
    private File workingDirectory = null;
    private Project project = null;
    private boolean newEnvironment = false;

    /** Controls whether the VM is used to launch commands, where possible */
    private boolean useVMLauncher = true;

    private static String antWorkingDirectory = System.getProperty("user.dir");
    private static CommandLauncher vmLauncher = null;
    private static CommandLauncher shellLauncher = null;
    private static Vector procEnvironment = null;

    /** Used to destroy processes when the VM exits. */
    private static ProcessDestroyer processDestroyer = new ProcessDestroyer();

    /**
     * Builds a command launcher for the OS and JVM we are running under
     */
    static {
        // Try using a JDK 1.3 launcher
        try {
            if (Os.isFamily("openvms")) {
                vmLauncher = new VmsCommandLauncher();
            } else if (!Os.isFamily("os/2")) {
                vmLauncher = new Java13CommandLauncher();
            }
        } catch (NoSuchMethodException exc) {
            // Ignore and keep trying
        }

        if (Os.isFamily("mac")) {
            // Mac
            shellLauncher = new MacCommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("os/2")) {
            // OS/2
            shellLauncher = new OS2CommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("windows")) {
            // Windows.  Need to determine which JDK we're running in

            CommandLauncher baseLauncher;
            if (System.getProperty("java.version").startsWith("1.1")) {
                // JDK 1.1
                baseLauncher = new Java11CommandLauncher();
            } else {
                // JDK 1.2
                baseLauncher = new CommandLauncher();
            }

            if (!Os.isFamily("win9x")) {
                // Windows XP/2000/NT
                shellLauncher = new WinNTCommandLauncher(baseLauncher);
            } else {
                // Windows 98/95 - need to use an auxiliary script
                shellLauncher
                    = new ScriptCommandLauncher("bin/antRun.bat", baseLauncher);
            }
        } else if (Os.isFamily("netware")) {
            // NetWare.  Need to determine which JDK we're running in
            CommandLauncher baseLauncher;
            if (System.getProperty("java.version").startsWith("1.1")) {
                // JDK 1.1
                baseLauncher = new Java11CommandLauncher();
            } else {
                // JDK 1.2
                baseLauncher = new CommandLauncher();
            }

            shellLauncher
                = new PerlScriptCommandLauncher("bin/antRun.pl", baseLauncher);
        } else if (Os.isFamily("openvms")) {
            // the vmLauncher already uses the shell
            shellLauncher = vmLauncher;
        } else {
            // Generic
            shellLauncher = new ScriptCommandLauncher("bin/antRun",
                new CommandLauncher());
        }
    }


    /**
     * Find the list of environment variables for this process.
     *
     * @return a vector containing the environment variables
     * the vector elements are strings formatted like variable = value
     */
    public static synchronized Vector getProcEnvironment() {
        if (procEnvironment != null) {
            return procEnvironment;
        }

        procEnvironment = new Vector();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Execute exe = new Execute(new PumpStreamHandler(out));
            exe.setCommandline(getProcEnvCommand());
            // Make sure we do not recurse forever
            exe.setNewenvironment(true);
            int retval = exe.execute();
            if (retval != 0) {
                // Just try to use what we got
            }

            BufferedReader in =
                new BufferedReader(new StringReader(toString(out)));

            if (Os.isFamily("openvms")) {
                procEnvironment = addVMSLogicals(procEnvironment, in);
                return procEnvironment;
            }

            String var = null;
            String line, lineSep = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                if (line.indexOf('=') == -1) {
                    // Chunk part of previous env var (UNIX env vars can
                    // contain embedded new lines).
                    if (var == null) {
                        var = lineSep + line;
                    } else {
                        var += lineSep + line;
                    }
                } else {
                    // New env var...append the previous one if we have it.
                    if (var != null) {
                        procEnvironment.addElement(var);
                    }
                    var = line;
                }
            }
            // Since we "look ahead" before adding, there's one last env var.
            if (var != null) {
                procEnvironment.addElement(var);
            }
        } catch (java.io.IOException exc) {
            exc.printStackTrace();
            // Just try to see how much we got
        }
        return procEnvironment;
    }

    private static String[] getProcEnvCommand() {
        if (Os.isFamily("os/2")) {
            // OS/2 - use same mechanism as Windows 2000
            String[] cmd = {"cmd", "/c", "set" };
            return cmd;
        } else if (Os.isFamily("windows")) {
            // Determine if we're running under XP/2000/NT or 98/95
            if (!Os.isFamily("win9x")) {
                // Windows XP/2000/NT
                String[] cmd = {"cmd", "/c", "set" };
                return cmd;
            } else {
                // Windows 98/95
                String[] cmd = {"command.com", "/c", "set" };
                return cmd;
            }
        } else if (Os.isFamily("z/os") || Os.isFamily("unix")) {
            // On most systems one could use: /bin/sh -c env

            // Some systems have /bin/env, others /usr/bin/env, just try
            String[] cmd = new String[1];
            if (new File("/bin/env").canRead()) {
                cmd[0] = "/bin/env";
            } else if (new File("/usr/bin/env").canRead()) {
                cmd[0] = "/usr/bin/env";
            } else {
                // rely on PATH
                cmd[0] = "env";
            }
            return cmd;
        } else if (Os.isFamily("netware") || Os.isFamily("os/400")) {
            // rely on PATH
            String[] cmd = {"env"};
            return cmd;
        } else if (Os.isFamily("openvms")) {
            String[] cmd = {"show", "logical"};
            return cmd;
        } else {
            // MAC OS 9 and previous
            //TODO: I have no idea how to get it, someone must fix it
            String[] cmd = null;
            return cmd;
        }
    }

    /**
     * ByteArrayOutputStream#toString doesn't seem to work reliably on
     * OS/390, at least not the way we use it in the execution
     * context.
     *
     * @param bos the output stream that one wants to read
     * @return the output stream as a string, read with
     * special encodings in the case of z/os and os/400
     *
     * @since Ant 1.5
     */
    public static String toString(ByteArrayOutputStream bos) {
        if (Os.isFamily("z/os")) {
            try {
                return bos.toString("Cp1047");
            } catch (java.io.UnsupportedEncodingException e) {
            }
        } else if (Os.isFamily("os/400")) {
            try {
                return bos.toString("Cp500");
            } catch (java.io.UnsupportedEncodingException e) {
            }
        }
        return bos.toString();
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
    public Execute(ExecuteStreamHandler streamHandler,
                   ExecuteWatchdog watchdog) {
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
        if (env == null || newEnvironment) {
            return env;
        }
        return patchEnvironment();
    }


    /**
     * Sets the environment variables for the subprocess to launch.
     *
     * @param env array of Strings, each element of which has
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
        if (wd == null || wd.getAbsolutePath().equals(antWorkingDirectory)) {
            workingDirectory = null;
        } else {
            workingDirectory = wd;
        }
    }

    /**
     * Set the name of the antRun script using the project's value.
     *
     * @param project the current project.
     *
     * @throws BuildException not clear when it is going to throw an exception, but
     * it is the method's signature
     */
    public void setAntRun(Project project) throws BuildException {
        this.project = project;
    }

    /**
     * Launch this execution through the VM, where possible, rather than through
     * the OS's shell. In some cases and operating systems using the shell will
     * allow the shell to perform additional processing such as associating an
     * executable with a script, etc
     *
     * @param useVMLauncher true if exec should launch through thge VM,
     *                   false if the shell should be used to launch the
     *                   command.
     */
    public void setVMLauncher(boolean useVMLauncher) {
        this.useVMLauncher = useVMLauncher;
    }

    /**
     * Creates a process that runs a command.
     *
     * @param project the Project, only used for logging purposes, may be null.
     * @param command the command to run
     * @param env the environment for the command
     * @param dir the working directory for the command
     * @param useVM use the built-in exec command for JDK 1.3 if available.
     * @return the process started
     * @throws IOException forwarded from the particular launcher used
     *
     * @since Ant 1.5
     */
    public static Process launch(Project project, String[] command,
                                 String[] env, File dir, boolean useVM)
        throws IOException {
        CommandLauncher launcher
            = vmLauncher != null ? vmLauncher : shellLauncher;
        if (!useVM) {
            launcher = shellLauncher;
        }

        return launcher.exec(project, command, env, dir);
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>
     * @exception java.io.IOException The exception is thrown, if launching
     *            of the subprocess failed
     */
    public int execute() throws IOException {
        final Process process = launch(project, getCommandline(),
                                       getEnvironment(), workingDirectory,
                                       useVMLauncher);

        try {
            streamHandler.setProcessInputStream(process.getOutputStream());
            streamHandler.setProcessOutputStream(process.getInputStream());
            streamHandler.setProcessErrorStream(process.getErrorStream());
        } catch (IOException e) {
            process.destroy();
            throw e;
        }
        streamHandler.start();

        try {
            // add the process to the list of those to destroy if the VM exits
            //
            processDestroyer.add(process);

            if (watchdog != null) {
                watchdog.start(process);
            }
            waitFor(process);

            if (watchdog != null) {
                watchdog.stop();
            }
            streamHandler.stop();

            if (watchdog != null) {
                watchdog.checkException();
            }
            return getExitValue();
        } finally {
            // remove the process to the list of those to destroy if the VM exits
            //
            processDestroyer.remove(process);
        }
    }

    /**
     * wait for a given process
     *
     * @param process the process one wants to wait for
     */
    protected void waitFor(Process process) {
        try {
            process.waitFor();
            setExitValue(process.exitValue());
        } catch (InterruptedException e) {
            process.destroy();
        }
    }

    /**
     * set the exit value
     *
     * @param value exit value of the process
     */
    protected void setExitValue(int value) {
        exitValue = value;
    }

    /**
     * Query the exit value of the process.
     * @return the exit value, 1 if the process was killed,
     * or Execute.INVALID if no exit value has been received
     */
    public int getExitValue() {
        return exitValue;
    }

    /**
     * Checks whether <code>exitValue</code> signals a failure on the current
     * system (OS specific).
     * @param exitValue the exit value (return code) to be checked
     * @return <code>true</code> if <code>exitValue</code> signals a failure
     */
    public static boolean isFailure(int exitValue) {
        if (Os.isFamily("openvms")) {
            // odd exit value signals failure
            return (exitValue % 2) == 0;
        } else {
            // non zero exit value signals failure
            return exitValue != 0;
        }
    }

    /**
     * test for an untimely death of the process
     * @return true iff a watchdog had to kill the process
     * @since Ant 1.5
     */
    public boolean killedProcess() {
        return watchdog != null && watchdog.killedProcess();
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
            String key = env[i].substring(0, pos + 1);
            int size = osEnv.size();
            for (int j = 0; j < size; j++) {
                if (((String) osEnv.elementAt(j)).startsWith(key)) {
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
    public static void runCommand(Task task, String[] cmdline)
        throws BuildException {
        try {
            task.log(Commandline.describeCommand(cmdline),
                     Project.MSG_VERBOSE);
            Execute exe = new Execute(new LogStreamHandler(task,
                                                           Project.MSG_INFO,
                                                           Project.MSG_ERR));
            exe.setAntRun(task.getProject());
            exe.setCommandline(cmdline);
            int retval = exe.execute();
            if (isFailure(retval)) {
                throw new BuildException(cmdline[0]
                    + " failed with return code " + retval, task.getLocation());
            }
        } catch (java.io.IOException exc) {
            throw new BuildException("Could not launch " + cmdline[0] + ": "
                + exc, task.getLocation());
        }
    }

    /**
     * This method is VMS specific and used by getProcEnvironment().
     *
     * Parses VMS logicals from <code>in</code> and adds them to
     * <code>environment</code>.  <code>in</code> is expected to be the
     * output of "SHOW LOGICAL".  The method takes care of parsing the output
     * correctly as well as making sure that a logical defined in multiple
     * tables only gets added from the highest order table.  Logicals with
     * multiple equivalence names are mapped to a variable with multiple
     * values separated by a comma (,).
     */
    private static Vector addVMSLogicals(Vector environment, BufferedReader in)
        throws IOException {
        HashMap logicals = new HashMap();
        String logName = null, logValue = null, newLogName;
        String line, lineSep = System.getProperty("line.separator");
        while ((line = in.readLine()) != null) {
            // parse the VMS logicals into required format ("VAR=VAL[,VAL2]")
            if (line.startsWith("\t=")) {
                // further equivalence name of previous logical
                if (logName != null) {
                    logValue += "," + line.substring(4, line.length() - 1);
                }
            } else if (line.startsWith("  \"")) {
                // new logical?
                if (logName != null) {
                    logicals.put(logName, logValue);
                }
                int eqIndex = line.indexOf('=');
                newLogName = line.substring(3, eqIndex - 2);
                if (logicals.containsKey(newLogName)) {
                    // already got this logical from a higher order table
                    logName = null;
                } else {
                    logName = newLogName;
                    logValue = line.substring(eqIndex + 3, line.length() - 1);
                }
            }
        }
        // Since we "look ahead" before adding, there's one last env var.
        if (logName != null) {
            logicals.put(logName, logValue);
        }

        for (Iterator i = logicals.keySet().iterator(); i.hasNext();) {
            String logical = (String) i.next();
            environment.add(logical + "=" + logicals.get(logical));
        }
        return environment;
    }

    /**
     * A command launcher for a particular JVM/OS platform.  This class is
     * a general purpose command launcher which can only launch commands in
     * the current working directory.
     */
    private static class CommandLauncher {
        /**
         * Launches the given command in a new process.
         *
         * @param project       The project that the command is part of
         * @param cmd           The command to execute
         * @param env           The environment for the new process.  If null,
         *                      the environment of the current proccess is used.
         * @throws IOException  if attempting to run a command in a specific directory
         */
        public Process exec(Project project, String[] cmd, String[] env)
             throws IOException {
            if (project != null) {
                project.log("Execute:CommandLauncher: "
                    + Commandline.describeCommand(cmd), Project.MSG_DEBUG);
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
         * @throws IOException  if trying to change directory
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            if (workingDir == null) {
                return exec(project, cmd, env);
            }
            throw new IOException("Cannot execute a process in different "
                + "directory under this JVM");
        }
    }

    /**
     * A command launcher for JDK/JRE 1.1 under Windows.  Fixes quoting problems
     * in Runtime.exec().  Can only launch commands in the current working
     * directory
     */
    private static class Java11CommandLauncher extends CommandLauncher {
        /**
         * Launches the given command in a new process.  Needs to quote
         * arguments
         * @param project the ant project
         * @param cmd the command line to execute as an array of strings
         * @param env the environment to set as an array of strings
         * @throws IOException probably forwarded from Runtime#exec
         */
        public Process exec(Project project, String[] cmd, String[] env)
             throws IOException {
            // Need to quote arguments with spaces, and to escape
            // quote characters
            String[] newcmd = new String[cmd.length];
            for (int i = 0; i < cmd.length; i++) {
                newcmd[i] = Commandline.quoteArgument(cmd[i]);
            }
            if (project != null) {
                project.log("Execute:Java11CommandLauncher: "
                    + Commandline.describeCommand(newcmd), Project.MSG_DEBUG);
            }
            return Runtime.getRuntime().exec(newcmd, env);
        }
    }

    /**
     * A command launcher for JDK/JRE 1.3 (and higher).  Uses the built-in
     * Runtime.exec() command
     */
    private static class Java13CommandLauncher extends CommandLauncher {
        public Java13CommandLauncher() throws NoSuchMethodException {
            // Locate method Runtime.exec(String[] cmdarray,
            //                            String[] envp, File dir)
            myExecWithCWD = Runtime.class.getMethod("exec",
                new Class[] {String[].class, String[].class, File.class});
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory
         * @param project the ant project
         * @param cmd the command line to execute as an array of strings
         * @param env the environment to set as an array of strings
         * @param workingDir the working directory where the command should run
         * @throws IOException probably forwarded from Runtime#exec
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            try {
                if (project != null) {
                    project.log("Execute:Java13CommandLauncher: "
                        + Commandline.describeCommand(cmd), Project.MSG_DEBUG);
                }
                Object[] arguments = {cmd, env, workingDir};
                return (Process) myExecWithCWD.invoke(Runtime.getRuntime(),
                                                     arguments);
            } catch (InvocationTargetException exc) {
                Throwable realexc = exc.getTargetException();
                if (realexc instanceof ThreadDeath) {
                    throw (ThreadDeath) realexc;
                } else if (realexc instanceof IOException) {
                    throw (IOException) realexc;
                } else {
                    throw new BuildException("Unable to execute command",
                                             realexc);
                }
            } catch (Exception exc) {
                // IllegalAccess, IllegalArgument, ClassCast
                throw new BuildException("Unable to execute command", exc);
            }
        }

        private Method myExecWithCWD;
    }

    /**
     * A command launcher that proxies another command launcher.
     *
     * Sub-classes override exec(args, env, workdir)
     */
    private static class CommandLauncherProxy extends CommandLauncher {
        CommandLauncherProxy(CommandLauncher launcher) {
            myLauncher = launcher;
        }

        /**
         * Launches the given command in a new process.  Delegates this
         * method to the proxied launcher
         * @param project the ant project
         * @param cmd the command line to execute as an array of strings
         * @param env the environment to set as an array of strings
         * @throws IOException forwarded from the exec method of the command launcher
         */
        public Process exec(Project project, String[] cmd, String[] env)
            throws IOException {
            return myLauncher.exec(project, cmd, env);
        }

        private CommandLauncher myLauncher;
    }

    /**
     * A command launcher for OS/2 that uses 'cmd.exe' when launching
     * commands in directories other than the current working
     * directory.
     *
     * <p>Unlike Windows NT and friends, OS/2's cd doesn't support the
     * /d switch to change drives and directories in one go.</p>
     */
    private static class OS2CommandLauncher extends CommandLauncherProxy {
        OS2CommandLauncher(CommandLauncher launcher) {
            super(launcher);
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory.
         * @param project the ant project
         * @param cmd the command line to execute as an array of strings
         * @param env the environment to set as an array of strings
         * @param workingDir working directory where the command should run
         * @throws IOException forwarded from the exec method of the command launcher
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            File commandDir = workingDir;
            if (workingDir == null) {
                if (project != null) {
                    commandDir = project.getBaseDir();
                } else {
                    return exec(project, cmd, env);
                }
            }

            // Use cmd.exe to change to the specified drive and
            // directory before running the command
            final int preCmdLength = 7;
            final String cmdDir = commandDir.getAbsolutePath();
            String[] newcmd = new String[cmd.length + preCmdLength];
            newcmd[0] = "cmd";
            newcmd[1] = "/c";
            newcmd[2] = cmdDir.substring(0, 2);
            newcmd[3] = "&&";
            newcmd[4] = "cd";
            newcmd[5] = cmdDir.substring(2);
            newcmd[6] = "&&";
            System.arraycopy(cmd, 0, newcmd, preCmdLength, cmd.length);

            return exec(project, newcmd, env);
        }
    }

    /**
     * A command launcher for Windows XP/2000/NT that uses 'cmd.exe' when
     * launching commands in directories other than the current working
     * directory.
     */
    private static class WinNTCommandLauncher extends CommandLauncherProxy {
        WinNTCommandLauncher(CommandLauncher launcher) {
            super(launcher);
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory.
         * @param project the ant project
         * @param cmd the command line to execute as an array of strings
         * @param env the environment to set as an array of strings
         * @param workingDir working directory where the command should run
         * @throws IOException forwarded from the exec method of the command launcher
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            File commandDir = workingDir;
            if (workingDir == null) {
                if (project != null) {
                    commandDir = project.getBaseDir();
                } else {
                    return exec(project, cmd, env);
                }
            }

            // Use cmd.exe to change to the specified directory before running
            // the command
            final int preCmdLength = 6;
            String[] newcmd = new String[cmd.length + preCmdLength];
            newcmd[0] = "cmd";
            newcmd[1] = "/c";
            newcmd[2] = "cd";
            newcmd[3] = "/d";
            newcmd[4] = commandDir.getAbsolutePath();
            newcmd[5] = "&&";
            System.arraycopy(cmd, 0, newcmd, preCmdLength, cmd.length);

            return exec(project, newcmd, env);
        }
    }

    /**
     * A command launcher for Mac that uses a dodgy mechanism to change
     * working directory before launching commands.
     */
    private static class MacCommandLauncher extends CommandLauncherProxy {
        MacCommandLauncher(CommandLauncher launcher) {
            super(launcher);
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory
         * @param project the ant project
         * @param cmd the command line to execute as an array of strings
         * @param env the environment to set as an array of strings
         * @param workingDir working directory where the command should run
         * @throws IOException forwarded from the exec method of the command launcher
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            if (workingDir == null) {
                return exec(project, cmd, env);
            }

            System.getProperties().put("user.dir", workingDir.getAbsolutePath());
            try {
                return exec(project, cmd, env);
            } finally {
                System.getProperties().put("user.dir", antWorkingDirectory);
            }
        }
    }

    /**
     * A command launcher that uses an auxiliary script to launch commands
     * in directories other than the current working directory.
     */
    private static class ScriptCommandLauncher extends CommandLauncherProxy {
        ScriptCommandLauncher(String script, CommandLauncher launcher) {
            super(launcher);
            myScript = script;
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            if (project == null) {
                if (workingDir == null) {
                    return exec(project, cmd, env);
                }
                throw new IOException("Cannot locate antRun script: "
                    + "No project provided");
            }

            // Locate the auxiliary script
            String antHome = project.getProperty("ant.home");
            if (antHome == null) {
                throw new IOException("Cannot locate antRun script: "
                    + "Property 'ant.home' not found");
            }
            String antRun = project.resolveFile(antHome + File.separator + myScript).toString();

            // Build the command
            File commandDir = workingDir;
            if (workingDir == null && project != null) {
                commandDir = project.getBaseDir();
            }

            String[] newcmd = new String[cmd.length + 2];
            newcmd[0] = antRun;
            newcmd[1] = commandDir.getAbsolutePath();
            System.arraycopy(cmd, 0, newcmd, 2, cmd.length);

            return exec(project, newcmd, env);
        }

        private String myScript;
    }

    /**
     * A command launcher that uses an auxiliary perl script to launch commands
     * in directories other than the current working directory.
     */
    private static class PerlScriptCommandLauncher
        extends CommandLauncherProxy {
        PerlScriptCommandLauncher(String script, CommandLauncher launcher) {
            super(launcher);
            myScript = script;
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            if (project == null) {
                if (workingDir == null) {
                    return exec(project, cmd, env);
                }
                throw new IOException("Cannot locate antRun script: "
                    + "No project provided");
            }

            // Locate the auxiliary script
            String antHome = project.getProperty("ant.home");
            if (antHome == null) {
                throw new IOException("Cannot locate antRun script: "
                    + "Property 'ant.home' not found");
            }
            String antRun = project.resolveFile(antHome + File.separator + myScript).toString();

            // Build the command
            File commandDir = workingDir;
            if (workingDir == null && project != null) {
                commandDir = project.getBaseDir();
            }

            String[] newcmd = new String[cmd.length + 3];
            newcmd[0] = "perl";
            newcmd[1] = antRun;
            newcmd[2] = commandDir.getAbsolutePath();
            System.arraycopy(cmd, 0, newcmd, 3, cmd.length);

            return exec(project, newcmd, env);
        }

        private String myScript;
    }

    /**
     * A command launcher for VMS that writes the command to a temporary DCL
     * script before launching commands.  This is due to limitations of both
     * the DCL interpreter and the Java VM implementation.
     */
    private static class VmsCommandLauncher extends Java13CommandLauncher {

        public VmsCommandLauncher() throws NoSuchMethodException {
            super();
        }

        /**
         * Launches the given command in a new process.
         */
        public Process exec(Project project, String[] cmd, String[] env)
            throws IOException {
            String[] vmsCmd = {createCommandFile(cmd).getPath()};
            return super.exec(project, vmsCmd, env);
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory.  Note that under Java 1.3.1, 1.4.0 and 1.4.1 on VMS this
         * method only works if <code>workingDir</code> is null or the logical
         * JAVA$FORK_SUPPORT_CHDIR needs to be set to TRUE.
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            String[] vmsCmd = {createCommandFile(cmd).getPath()};
            return super.exec(project, vmsCmd, env, workingDir);
        }

        /*
         * Writes the command into a temporary DCL script and returns the
         * corresponding File object.  The script will be deleted on exit.
         */
        private File createCommandFile(String[] cmd) throws IOException {
            File script = File.createTempFile("ANT", ".COM");
            script.deleteOnExit();
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(script));
                StringBuffer dclCmd = new StringBuffer("$");
                for (int i = 0; i < cmd.length; i++) {
                    dclCmd.append(' ').append(cmd[i]);
                }
                out.println(dclCmd.toString());
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            return script;
        }

    }
}
