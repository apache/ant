/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.taskdefs.launcher.CommandLauncher;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;

/**
 * Runs an external program.
 *
 * @since Ant 1.2
 */
public class Execute {

    private static final int ONE_SECOND = 1000;

    /**
     * Invalid exit code. set to {@link Integer#MAX_VALUE}
     */
    public static final int INVALID = Integer.MAX_VALUE;

    private static String antWorkingDirectory = System.getProperty("user.dir");
    private static Map<String, String> procEnvironment = null;

    /** Used to destroy processes when the VM exits. */
    private static ProcessDestroyer processDestroyer = new ProcessDestroyer();

    /** Used for replacing env variables */
    private static boolean environmentCaseInSensitive = false;

    static {
        if (Os.isFamily("windows")) {
            environmentCaseInSensitive = true;
        }
    }

    private String[] cmdl = null;
    private String[] env = null;
    private int exitValue = INVALID;
    private ExecuteStreamHandler streamHandler;
    private final ExecuteWatchdog watchdog;
    private File workingDirectory = null;
    private Project project = null;
    private boolean newEnvironment = false;

    /** Controls whether the VM is used to launch commands, where possible. */
    private boolean useVMLauncher = true;

    /**
     * Set whether or not you want the process to be spawned.
     * Default is not spawned.
     *
     * @param spawn if true you do not want Ant
     *              to wait for the end of the process.
     *              Has no influence in here, the calling task contains
     *              and acts accordingly
     *
     * @since Ant 1.6
     * @deprecated
     */
    @Deprecated
    public void setSpawn(boolean spawn) {
        // Method did not do anything to begin with
    }

    /**
     * Find the list of environment variables for this process.
     *
     * @return a map containing the environment variables.
     * @since Ant 1.8.2
     */
    public static synchronized Map<String, String> getEnvironmentVariables() {
        if (procEnvironment != null) {
            return procEnvironment;
        }
        if (!Os.isFamily("openvms")) {
            try {
                procEnvironment = System.getenv();
                return procEnvironment;
            } catch (Exception x) {
                x.printStackTrace(); //NOSONAR
            }
        }

        procEnvironment = new LinkedHashMap<>();
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
                procEnvironment = getVMSLogicals(in);
                return procEnvironment;
            }
            StringBuilder var = null;
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("=")) {
                    // New env var...append the previous one if we have it.
                    if (var != null) {
                        int eq = var.toString().indexOf('=');
                        procEnvironment.put(var.substring(0, eq),
                                            var.substring(eq + 1));
                    }
                    var = new StringBuilder(line);
                } else {
                    // Chunk part of previous env var (UNIX env vars can
                    // contain embedded new lines).
                    if (var == null) {
                        var = new StringBuilder(System.lineSeparator() + line);
                    } else {
                        var.append(System.lineSeparator()).append(line);
                    }
                }
            }
            // Since we "look ahead" before adding, there's one last env var.
            if (var != null) {
                int eq = var.toString().indexOf('=');
                procEnvironment.put(var.substring(0, eq), var.substring(eq + 1));
            }
        } catch (IOException exc) {
            exc.printStackTrace(); //NOSONAR
            // Just try to see how much we got
        }
        return procEnvironment;
    }

    /**
     * Find the list of environment variables for this process.
     *
     * @return a vector containing the environment variables.
     * The vector elements are strings formatted like variable = value.
     * @deprecated use #getEnvironmentVariables instead
     */
    @Deprecated
    public static synchronized Vector<String> getProcEnvironment() {
        Vector<String> v = new Vector<>();
        getEnvironmentVariables().forEach((key, value) -> v.add(key + "=" + value));
        return v;
    }

    /**
     * This is the operation to get our environment.
     * It is a notorious troublespot pre-Java1.5, and should be approached
     * with extreme caution.
     *
     * @return command and arguments to get our environment
     */
    private static String[] getProcEnvCommand() {
        if (Os.isFamily("os/2")) {
            // OS/2 - use same mechanism as Windows 2000
            return new String[] {"cmd", "/c", "set"};
        }
        if (Os.isFamily("windows")) {
            // Determine if we're running under XP/2000/NT or 98/95
            if (Os.isFamily("win9x")) {
                // Windows 98/95
                return new String[] {"command.com", "/c", "set"};
            }
            // Windows XP/2000/NT/2003
            return new String[] {"cmd", "/c", "set"};
        }
        if (Os.isFamily("z/os") || Os.isFamily("unix")) {
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
        }
        if (Os.isFamily("netware") || Os.isFamily("os/400")) {
            // rely on PATH
            return new String[] {"env"};
        }
        if (Os.isFamily("openvms")) {
            return new String[] {"show", "logical"};
        }
        // MAC OS 9 and previous
        // TODO: I have no idea how to get it, someone must fix it
        return null;
    }

    /**
     * ByteArrayOutputStream#toString doesn't seem to work reliably on
     * OS/390, at least not the way we use it in the execution
     * context.
     *
     * @param bos the output stream that one wants to read.
     * @return the output stream as a string, read with
     * special encodings in the case of z/os and os/400.
     * @since Ant 1.5
     */
    public static String toString(ByteArrayOutputStream bos) {
        if (Os.isFamily("z/os")) {
            try {
                return bos.toString("Cp1047");
            } catch (UnsupportedEncodingException e) {
                // noop default encoding used
            }
        } else if (Os.isFamily("os/400")) {
            try {
                return bos.toString("Cp500");
            } catch (UnsupportedEncodingException e) {
                // noop default encoding used
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
     * @param watchdog a watchdog for the subprocess or <code>null</code>
     *        to disable a timeout for the subprocess.
     */
    public Execute(ExecuteStreamHandler streamHandler,
                   ExecuteWatchdog watchdog) {
        setStreamHandler(streamHandler);
        this.watchdog = watchdog;
        // By default, use the shell launcher for VMS
        //
        if (Os.isFamily("openvms")) {
            useVMLauncher = false;
        }
    }

    /**
     * Set the stream handler to use.
     *
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
        workingDirectory = wd;
    }

    /**
     * Return the working directory.
     *
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
     * @since Ant 1.5
     */
    public static Process launch(Project project, String[] command,
                                 String[] env, File dir, boolean useVM)
        throws IOException {
        if (dir != null && !dir.exists()) {
            throw new BuildException("%s doesn't exist.", dir);
        }

        CommandLauncher vmLauncher = CommandLauncher.getVMLauncher(project);
        CommandLauncher launcher = (useVM && vmLauncher != null)
            ? vmLauncher : CommandLauncher.getShellLauncher(project);
        return launcher.exec(project, command, env, dir);
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>.
     * @exception IOException The exception is thrown, if launching
     *            of the subprocess failed.
     */
    public int execute() throws IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new BuildException("%s doesn't exist.", workingDirectory);
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
     * @throws IOException The exception is thrown, if launching
     *            of the subprocess failed.
     * @since Ant 1.6
     */
    public void spawn() throws IOException {
        if (workingDirectory != null && !workingDirectory.exists()) {
            throw new BuildException("%s doesn't exist.", workingDirectory);
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
            @Override
            public void write(int b) throws IOException {
                // Method intended to swallow whatever comes at it
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
     *
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
     * running doesn't follow these conventions. One notable
     * exception is the Java VM provided by HP for OpenVMS - it will
     * return 0 if successful (like on any other platform), but this
     * signals a failure on OpenVMS. So if you execute a new Java VM
     * on OpenVMS, you cannot trust this method.</p>
     *
     * @param exitValue the exit value (return code) to be checked.
     * @return <code>true</code> if <code>exitValue</code> signals a failure.
     */
    public static boolean isFailure(int exitValue) {
        // on openvms even exit value signals failure;
        // for other platforms nonzero exit value signals failure
        return Os.isFamily("openvms")
            ? (exitValue % 2 == 0) : (exitValue != 0);
    }

    /**
     * Did this execute return in a failure.
     *
     * @see #isFailure(int)
     * @return true if and only if the exit code is interpreted as a failure
     * @since Ant1.7
     */
    public boolean isFailure() {
        return isFailure(getExitValue());
    }

    /**
     * Test for an untimely death of the process.
     *
     * @return true if a watchdog had to kill the process.
     * @since Ant 1.5
     */
    public boolean killedProcess() {
        return watchdog != null && watchdog.killedProcess();
    }

    /**
     * Patch the current environment with the new values from the user.
     *
     * @return the patched environment.
     */
    private String[] patchEnvironment() {
        // On OpenVMS Runtime#exec() doesn't support the environment array,
        // so we only return the new values which then will be set in
        // the generated DCL script, inheriting the parent process environment
        if (Os.isFamily("openvms")) {
            return env;
        }
        Map<String, String> osEnv =
            new LinkedHashMap<>(getEnvironmentVariables());
        for (String keyValue : env) {
            String key = keyValue.substring(0, keyValue.indexOf('='));
            // Find the key in the current environment copy
            // and remove it.

            // Try without changing case first
            if (osEnv.remove(key) == null && environmentCaseInSensitive) {
                // not found, maybe perform a case insensitive search

                for (String osEnvItem : osEnv.keySet()) {
                    // Nb: using default locale as key is a env name
                    if (osEnvItem.equalsIgnoreCase(key)) {
                        // Use the original case of the key
                        key = osEnvItem;
                        break;
                    }
                }
            }

            // Add the key to the environment copy
            osEnv.put(key, keyValue.substring(key.length() + 1));
        }

        return osEnv.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    /**
     * A utility method that runs an external command. Writes the output and
     * error streams of the command to the project log.
     *
     * @param task The task that the command is part of. Used for logging
     * @param cmdline The command to execute.
     * @throws BuildException if the command does not exit successfully.
     */
    public static void runCommand(Task task, String... cmdline)
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
        } catch (IOException exc) {
            throw new BuildException("Could not launch " + cmdline[0] + ": "
                + exc, task.getLocation());
        }
    }

    /**
     * Close the streams belonging to the given Process.
     *
     * @param process the <code>Process</code>.
     */
    public static void closeStreams(Process process) {
        FileUtils.close(process.getInputStream());
        FileUtils.close(process.getOutputStream());
        FileUtils.close(process.getErrorStream());
    }

    /**
     * This method is VMS specific and used by getEnvironmentVariables().
     *
     * Parses VMS logicals from <code>in</code> and returns them as a Map.
     * <code>in</code> is expected to be the
     * output of "SHOW LOGICAL". The method takes care of parsing the output
     * correctly as well as making sure that a logical defined in multiple
     * tables only gets added from the highest order table. Logicals with
     * multiple equivalence names are mapped to a variable with multiple
     * values separated by a comma (,).
     */
    private static Map<String, String> getVMSLogicals(BufferedReader in)
        throws IOException {
        Map<String, String> logicals = new HashMap<>();
        String logName = null, logValue = null, newLogName;
        String line;
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
        return logicals;
    }
}
