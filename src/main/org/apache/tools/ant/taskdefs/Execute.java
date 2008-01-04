/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Runs an external program.
 *
 * @since Ant 1.2
 *
 */
public class Execute {

    private static final int ONE_SECOND = 1000;

    /** Invalid exit code.
     * set to {@link Integer#MAX_VALUE}
     */
    public static final int INVALID = Integer.MAX_VALUE;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private String[] cmdl = null;
    private String[] env = null;
    private int exitValue = INVALID;
    private ExecuteStreamHandler streamHandler;
    private ExecuteWatchdog watchdog;
    private File workingDirectory = null;
    private Project project = null;
    private boolean newEnvironment = false;
    //TODO: nothing appears to read this but is set using a public setter.
    private boolean spawn = false;


    /** Controls whether the VM is used to launch commands, where possible. */
    private boolean useVMLauncher = true;

    private static String antWorkingDirectory = System.getProperty("user.dir");
    private static CommandLauncher vmLauncher = null;
    private static CommandLauncher shellLauncher = null;
    private static Vector procEnvironment = null;

    /** Used to destroy processes when the VM exits. */
    private static ProcessDestroyer processDestroyer = new ProcessDestroyer();

    /** Used for replacing env variables */
    private static boolean environmentCaseInSensitive = false;

    /*
     * Builds a command launcher for the OS and JVM we are running under.
     */
    static {
        // Try using a JDK 1.3 launcher
        try {
            if (!Os.isFamily("os/2")) {
                vmLauncher = new Java13CommandLauncher();
            }
        } catch (NoSuchMethodException exc) {
            // Ignore and keep trying
        }
        if (Os.isFamily("mac") && !Os.isFamily("unix")) {
            // Mac
            shellLauncher = new MacCommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("os/2")) {
            // OS/2
            shellLauncher = new OS2CommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("windows")) {
            environmentCaseInSensitive = true;
            CommandLauncher baseLauncher = new CommandLauncher();

            if (!Os.isFamily("win9x")) {
                // Windows XP/2000/NT
                shellLauncher = new WinNTCommandLauncher(baseLauncher);
            } else {
                // Windows 98/95 - need to use an auxiliary script
                shellLauncher
                    = new ScriptCommandLauncher("bin/antRun.bat", baseLauncher);
            }
        } else if (Os.isFamily("netware")) {

            CommandLauncher baseLauncher = new CommandLauncher();

            shellLauncher
                = new PerlScriptCommandLauncher("bin/antRun.pl", baseLauncher);
        } else if (Os.isFamily("openvms")) {
            // OpenVMS
            try {
                shellLauncher = new VmsCommandLauncher();
            } catch (NoSuchMethodException exc) {
            // Ignore and keep trying
            }
        } else {
            // Generic
            shellLauncher = new ScriptCommandLauncher("bin/antRun",
                new CommandLauncher());
        }
    }

    /**
     * Set whether or not you want the process to be spawned.
     * Default is not spawned.
     *
     * @param spawn if true you do not want Ant
     *              to wait for the end of the process.
     *
     * @since Ant 1.6
     */
    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    /**
     * Find the list of environment variables for this process.
     *
     * @return a vector containing the environment variables.
     * The vector elements are strings formatted like variable = value.
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
            String line, lineSep = StringUtils.LINE_SEP;
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

    /**
     * This is the operation to get our environment.
     * It is a notorious troublespot pre-Java1.5, and should be approached
     * with extreme caution.
     * @return
     */
    private static String[] getProcEnvCommand() {
        if (Os.isFamily("os/2")) {
            // OS/2 - use same mechanism as Windows 2000
            return new String[] {"cmd", "/c", "set" };
        } else if (Os.isFamily("windows")) {
            // Determine if we're running under XP/2000/NT or 98/95
            if (Os.isFamily("win9x")) {
                // Windows 98/95
                return new String[] {"command.com", "/c", "set" };
            } else {
                // Windows XP/2000/NT/2003
                return new String[] {"cmd", "/c", "set" };
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
            return new String[] {"env"};
        } else if (Os.isFamily("openvms")) {
            return new String[] {"show", "logical"};
        } else {
            // MAC OS 9 and previous
            //TODO: I have no idea how to get it, someone must fix it
            return null;
        }
    }

    /**
     * ByteArrayOutputStream#toString doesn't seem to work reliably on
     * OS/390, at least not the way we use it in the execution
     * context.
     *
     * @param bos the output stream that one wants to read.
     * @return the output stream as a string, read with
     * special encodings in the case of z/os and os/400.
     *
     * @since Ant 1.5
     */
    public static String toString(ByteArrayOutputStream bos) {
        if (Os.isFamily("z/os")) {
            try {
                return bos.toString("Cp1047");
            } catch (java.io.UnsupportedEncodingException e) {
                //noop default encoding used
            }
        } else if (Os.isFamily("os/400")) {
            try {
                return bos.toString("Cp500");
            } catch (java.io.UnsupportedEncodingException e) {
                //noop default encoding used
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
        setStreamHandler(streamHandler);
        this.watchdog = watchdog;
        //By default, use the shell launcher for VMS
        //
        if (Os.isFamily("openvms")) {
            useVMLauncher = false;
        }
    }

    /**
     * Set the stream handler to use.
     * @param streamHandler ExecuteStreamHandler.
     * @since Ant 1.6
     */
    public void setStreamHandler(ExecuteStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    /**
     * Returns the commandline used to create a subprocess.
     *
     * @return the commandline used to create a subprocess.
     */
    public String[] getCommandline() {
        return cmdl;
    }

    /**
     * Sets the commandline of the subprocess to launch.
     *
     * @param commandline the commandline of the subprocess to launch.
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
     * @return the environment used to create a subprocess.
     */
    public String[] getEnvironment() {
        return (env == null || newEnvironment)
            ? env : patchEnvironment();
    }

    /**
     * Sets the environment variables for the subprocess to launch.
     *
     * @param env array of Strings, each element of which has
     * an environment variable settings in format <em>key=value</em>.
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
        workingDirectory =
            (wd == null || wd.getAbsolutePath().equals(antWorkingDirectory))
            ? null : wd;
    }

    /**
     * Return the working directory.
     * @return the directory as a File.
     * @since Ant 1.7
     */
    public File getWorkingDirectory() {
        return workingDirectory == null ? new File(antWorkingDirectory)
                                        : workingDirectory;
    }

    /**
     * Set the name of the antRun script using the project's value.
     *
     * @param project the current project.
     *
     * @throws BuildException not clear when it is going to throw an exception, but
     * it is the method's signature.
     */
    public void setAntRun(Project project) throws BuildException {
        this.project = project;
    }

    /**
     * Launch this execution through the VM, where possible, rather than through
     * the OS's shell. In some cases and operating systems using the shell will
     * allow the shell to perform additional processing such as associating an
     * executable with a script, etc.
     *
     * @param useVMLauncher true if exec should launch through the VM,
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
     * @param command the command to run.
     * @param env the environment for the command.
     * @param dir the working directory for the command.
     * @param useVM use the built-in exec command for JDK 1.3 if available.
     * @return the process started.
     * @throws IOException forwarded from the particular launcher used.
     *
     * @since Ant 1.5
     */
    public static Process launch(Project project, String[] command,
                                 String[] env, File dir, boolean useVM)
        throws IOException {
        if (dir != null && !dir.exists()) {
            throw new BuildException(dir + " doesn't exist.");
        }
        CommandLauncher launcher
            = ((useVM && vmLauncher != null) ? vmLauncher : shellLauncher);
        return launcher.exec(project, command, env, dir);
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>.
     * @exception java.io.IOException The exception is thrown, if launching
     *            of the subprocess failed.
     */
    public int execute() throws IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new BuildException(workingDirectory + " doesn't exist.");
        }
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
            closeStreams(process);

            if (watchdog != null) {
                watchdog.checkException();
            }
            return getExitValue();
        } catch (ThreadDeath t) {
            // #31928: forcibly kill it before continuing.
            process.destroy();
            throw t;
        } finally {
            // remove the process to the list of those to destroy if
            // the VM exits
            //
            processDestroyer.remove(process);
        }
    }

    /**
     * Starts a process defined by the command line.
     * Ant will not wait for this process, nor log its output.
     *
     * @throws java.io.IOException The exception is thrown, if launching
     *            of the subprocess failed.
     * @since Ant 1.6
     */
    public void spawn() throws IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new BuildException(workingDirectory + " doesn't exist.");
        }
        final Process process = launch(project, getCommandline(),
                                       getEnvironment(), workingDirectory,
                                       useVMLauncher);
        if (Os.isFamily("windows")) {
            try {
                Thread.sleep(ONE_SECOND);
            } catch (InterruptedException e) {
                project.log("interruption in the sleep after having spawned a"
                            + " process", Project.MSG_VERBOSE);
            }
        }
        OutputStream dummyOut = new OutputStream() {
            public void write(int b) throws IOException {
            }
        };

        ExecuteStreamHandler handler = new PumpStreamHandler(dummyOut);
        handler.setProcessErrorStream(process.getErrorStream());
        handler.setProcessOutputStream(process.getInputStream());
        handler.start();
        process.getOutputStream().close();

        project.log("spawned process " + process.toString(),
                    Project.MSG_VERBOSE);
    }

    /**
     * Wait for a given process.
     *
     * @param process the process one wants to wait for.
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
     * Set the exit value.
     *
     * @param value exit value of the process.
     */
    protected void setExitValue(int value) {
        exitValue = value;
    }

    /**
     * Query the exit value of the process.
     * @return the exit value or Execute.INVALID if no exit value has
     * been received.
     */
    public int getExitValue() {
        return exitValue;
    }

    /**
     * Checks whether <code>exitValue</code> signals a failure on the current
     * system (OS specific).
     *
     * <p><b>Note</b> that this method relies on the conventions of
     * the OS, it will return false results if the application you are
     * running doesn't follow these conventions.  One notable
     * exception is the Java VM provided by HP for OpenVMS - it will
     * return 0 if successful (like on any other platform), but this
     * signals a failure on OpenVMS.  So if you execute a new Java VM
     * on OpenVMS, you cannot trust this method.</p>
     *
     * @param exitValue the exit value (return code) to be checked.
     * @return <code>true</code> if <code>exitValue</code> signals a failure.
     */
    public static boolean isFailure(int exitValue) {
        //on openvms even exit value signals failure;
        // for other platforms nonzero exit value signals failure
        return Os.isFamily("openvms")
            ? (exitValue % 2 == 0) : (exitValue != 0);
    }

    /**
     * Did this execute return in a failure.
     * @see #isFailure(int)
     * @return true if and only if the exit code is interpreted as a failure
     * @since Ant1.7
     */
    public boolean isFailure() {
        return isFailure(getExitValue());
    }

    /**
     * Test for an untimely death of the process.
     * @return true if a watchdog had to kill the process.
     * @since Ant 1.5
     */
    public boolean killedProcess() {
        return watchdog != null && watchdog.killedProcess();
    }

    /**
     * Patch the current environment with the new values from the user.
     * @return the patched environment.
     */
    private String[] patchEnvironment() {
        // On OpenVMS Runtime#exec() doesn't support the environment array,
        // so we only return the new values which then will be set in
        // the generated DCL script, inheriting the parent process environment
        if (Os.isFamily("openvms")) {
            return env;
        }
        Vector osEnv = (Vector) getProcEnvironment().clone();
        for (int i = 0; i < env.length; i++) {
            String keyValue = env[i];
            // Get key including "="
            String key = keyValue.substring(0, keyValue.indexOf('=') + 1);
            if (environmentCaseInSensitive) {
                // Nb: using default locale as key is a env name
                key = key.toLowerCase();
            }
            int size = osEnv.size();
            // Find the key in the current enviroment copy
            // and remove it.
            for (int j = 0; j < size; j++) {
                String osEnvItem = (String) osEnv.elementAt(j);
                String convertedItem = environmentCaseInSensitive
                    ? osEnvItem.toLowerCase() : osEnvItem;
                if (convertedItem.startsWith(key)) {
                    osEnv.removeElementAt(j);
                    if (environmentCaseInSensitive) {
                        // Use the original casiness of the key
                        keyValue = osEnvItem.substring(0, key.length())
                            + keyValue.substring(key.length());
                    }
                    break;
                }
            }
            // Add the key to the enviromnent copy
            osEnv.addElement(keyValue);
        }
        return (String[]) (osEnv.toArray(new String[osEnv.size()]));
    }

    /**
     * A utility method that runs an external command.  Writes the output and
     * error streams of the command to the project log.
     *
     * @param task      The task that the command is part of.  Used for logging
     * @param cmdline   The command to execute.
     *
     * @throws BuildException if the command does not exit successfully.
     */
    public static void runCommand(Task task, String[] cmdline)
        throws BuildException {
        try {
            task.log(Commandline.describeCommand(cmdline),
                     Project.MSG_VERBOSE);
            Execute exe = new Execute(
                new LogStreamHandler(task, Project.MSG_INFO, Project.MSG_ERR));
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
     * Close the streams belonging to the given Process.
     * @param process   the <code>Process</code>.
     */
    public static void closeStreams(Process process) {
        FileUtils.close(process.getInputStream());
        FileUtils.close(process.getOutputStream());
        FileUtils.close(process.getErrorStream());
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
        String line = null;
        // CheckStyle:MagicNumber OFF
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
        // CheckStyle:MagicNumber ON
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
         * @param project       The project that the command is part of.
         * @param cmd           The command to execute.
         * @param env           The environment for the new process.  If null,
         *                      the environment of the current process is used.
         * @return the created Process.
         * @throws IOException if attempting to run a command in a
         * specific directory.
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
         * @param project       The project that the command is part of.
         * @param cmd           The command to execute.
         * @param env           The environment for the new process.  If null,
         *                      the environment of the current process is used.
         * @param workingDir    The directory to start the command in.  If null,
         *                      the current directory is used.
         * @return the created Process.
         * @throws IOException  if trying to change directory.
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
     * A command launcher for JDK/JRE 1.3 (and higher).  Uses the built-in
     * Runtime.exec() command.
     */
    private static class Java13CommandLauncher extends CommandLauncher {
        private Method myExecWithCWD;

        public Java13CommandLauncher() throws NoSuchMethodException {
            // Locate method Runtime.exec(String[] cmdarray,
            //                            String[] envp, File dir)
            myExecWithCWD = Runtime.class.getMethod("exec",
                new Class[] {String[].class, String[].class, File.class});
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory.
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir the working directory where the command
         * should run.
         * @return the created Process.
         * @throws IOException probably forwarded from Runtime#exec.
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            try {
                if (project != null) {
                    project.log("Execute:Java13CommandLauncher: "
                        + Commandline.describeCommand(cmd), Project.MSG_DEBUG);
                }
                return (Process) myExecWithCWD.invoke(Runtime.getRuntime(),
                   /* the arguments: */ new Object[] {cmd, env, workingDir});
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
    }

    /**
     * A command launcher that proxies another command launcher.
     *
     * Sub-classes override exec(args, env, workdir).
     */
    private static class CommandLauncherProxy extends CommandLauncher {
        private CommandLauncher myLauncher;

        CommandLauncherProxy(CommandLauncher launcher) {
            myLauncher = launcher;
        }

        /**
         * Launches the given command in a new process.  Delegates this
         * method to the proxied launcher.
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
         */
        public Process exec(Project project, String[] cmd, String[] env)
            throws IOException {
            return myLauncher.exec(project, cmd, env);
        }
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
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir working directory where the command should run.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
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
            // CheckStyle:MagicNumber OFF - do not bother
            newcmd[0] = "cmd";
            newcmd[1] = "/c";
            newcmd[2] = cmdDir.substring(0, 2);
            newcmd[3] = "&&";
            newcmd[4] = "cd";
            newcmd[5] = cmdDir.substring(2);
            newcmd[6] = "&&";
            // CheckStyle:MagicNumber ON
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
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir working directory where the command should run.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
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
            // CheckStyle:MagicNumber OFF - do not bother
            newcmd[0] = "cmd";
            newcmd[1] = "/c";
            newcmd[2] = "cd";
            newcmd[3] = "/d";
            newcmd[4] = commandDir.getAbsolutePath();
            newcmd[5] = "&&";
            // CheckStyle:MagicNumber ON
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
         * directory.
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir working directory where the command should run.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
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
         * directory.
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir working directory where the command should run.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
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
            String antHome = project.getProperty(MagicNames.ANT_HOME);
            if (antHome == null) {
                throw new IOException("Cannot locate antRun script: "
                    + "Property '" + MagicNames.ANT_HOME + "' not found");
            }
            String antRun =
                FILE_UTILS.resolveFile(project.getBaseDir(),
                        antHome + File.separator + myScript).toString();

            // Build the command
            File commandDir = workingDir;
            if (workingDir == null) {
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
        private String myScript;

        PerlScriptCommandLauncher(String script, CommandLauncher launcher) {
            super(launcher);
            myScript = script;
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory.
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir working directory where the command should run.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
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
            String antHome = project.getProperty(MagicNames.ANT_HOME);
            if (antHome == null) {
                throw new IOException("Cannot locate antRun script: "
                    + "Property '" + MagicNames.ANT_HOME + "' not found");
            }
            String antRun =
                FILE_UTILS.resolveFile(project.getBaseDir(),
                        antHome + File.separator + myScript).toString();

            // Build the command
            File commandDir = workingDir;
            if (workingDir == null) {
                commandDir = project.getBaseDir();
            }
            // CheckStyle:MagicNumber OFF
            String[] newcmd = new String[cmd.length + 3];
            newcmd[0] = "perl";
            newcmd[1] = antRun;
            newcmd[2] = commandDir.getAbsolutePath();
            System.arraycopy(cmd, 0, newcmd, 3, cmd.length);
            // CheckStyle:MagicNumber ON

            return exec(project, newcmd, env);
        }
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
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
         */
        public Process exec(Project project, String[] cmd, String[] env)
            throws IOException {
            File cmdFile = createCommandFile(cmd, env);
            Process p
                = super.exec(project, new String[] {cmdFile.getPath()}, env);
            deleteAfter(cmdFile, p);
            return p;
        }

        /**
         * Launches the given command in a new process, in the given working
         * directory.  Note that under Java 1.3.1, 1.4.0 and 1.4.1 on VMS this
         * method only works if <code>workingDir</code> is null or the logical
         * JAVA$FORK_SUPPORT_CHDIR needs to be set to TRUE.
         * @param project the Ant project.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @param workingDir working directory where the command should run.
         * @return the created Process.
         * @throws IOException forwarded from the exec method of the
         * command launcher.
         */
        public Process exec(Project project, String[] cmd, String[] env,
                            File workingDir) throws IOException {
            File cmdFile = createCommandFile(cmd, env);
            Process p = super.exec(project, new String[] {cmdFile.getPath()},
                                   env, workingDir);
            deleteAfter(cmdFile, p);
            return p;
        }

        /*
         * Writes the command into a temporary DCL script and returns the
         * corresponding File object.  The script will be deleted on exit.
         * @param cmd the command line to execute as an array of strings.
         * @param env the environment to set as an array of strings.
         * @return the command File.
         * @throws IOException if errors are encountered creating the file.
         */
        private File createCommandFile(String[] cmd, String[] env)
            throws IOException {
            File script = FILE_UTILS.createTempFile("ANT", ".COM", null, true, true);
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(script));

                // add the environment as logicals to the DCL script
                if (env != null) {
                    int eqIndex;
                    for (int i = 0; i < env.length; i++) {
                        eqIndex = env[i].indexOf('=');
                        if (eqIndex != -1) {
                            out.print("$ DEFINE/NOLOG ");
                            out.print(env[i].substring(0, eqIndex));
                            out.print(" \"");
                            out.print(env[i].substring(eqIndex + 1));
                            out.println('\"');
                        }
                    }
                }
                out.print("$ " + cmd[0]);
                for (int i = 1; i < cmd.length; i++) {
                    out.println(" -");
                    out.print(cmd[i]);
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            return script;
        }

        private void deleteAfter(final File f, final Process p) {
            new Thread() {
                public void run() {
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    FileUtils.delete(f);
                }
            }
            .start();
        }
    }
}
