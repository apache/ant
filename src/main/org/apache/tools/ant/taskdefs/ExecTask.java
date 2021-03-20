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

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.util.FileUtils;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * @since Ant 1.2
 *
 * @ant.task category="control"
 */
public class ExecTask extends Task {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private String os;
    private String osFamily;

    private File dir;
    // CheckStyle:VisibilityModifier OFF - bc
    protected boolean failOnError = false;
    protected boolean newEnvironment = false;
    private Long timeout = null;
    private Environment env = new Environment();
    protected Commandline cmdl = new Commandline();
    private String resultProperty;
    private boolean failIfExecFails = true;
    private String executable;
    private boolean resolveExecutable = false;
    private boolean searchPath = false;
    private boolean spawn = false;
    private boolean incompatibleWithSpawn = false;

    //include locally for screening purposes
    private String inputString;
    private File input;
    private File output;
    private File error;

    protected Redirector redirector = new Redirector(this);
    protected RedirectorElement redirectorElement;
    // CheckStyle:VisibilityModifier ON

    /**
     * Controls whether the VM (1.3 and above) is used to execute the
     * command
     */
    private boolean vmLauncher = true;


    /**
     * Create an instance.
     * Needs to be configured by binding to a project.
     */
    public ExecTask() {
    }

    /**
     * create an instance that is helping another task.
     * Project, OwningTarget, TaskName and description are all
     * pulled out
     * @param owner task that we belong to
     */
    public ExecTask(Task owner) {
        bindToOwner(owner);
    }

    /**
     * Set whether or not you want the process to be spawned.
     * Default is false.
     * @param spawn if true you do not want Ant to wait for the end of the process.
     * @since Ant 1.6
     */
    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    /**
     * Set the timeout in milliseconds after which the process will be killed.
     *
     * @param value timeout in milliseconds.
     *
     * @since Ant 1.5
     */
    public void setTimeout(Long value) {
        timeout = value;
        incompatibleWithSpawn |= timeout != null;
    }

    /**
     * Set the timeout in milliseconds after which the process will be killed.
     *
     * @param value timeout in milliseconds.
     */
    public void setTimeout(Integer value) {
        setTimeout(
            (value == null) ? null : (long) value);
    }

    /**
     * Set the name of the executable program.
     * @param value the name of the executable program.
     */
    public void setExecutable(String value) {
        this.executable = value;
        cmdl.setExecutable(value);
    }

    /**
     * Set the working directory of the process.
     * @param d the working directory of the process.
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * List of operating systems on which the command may be executed.
     * @param os list of operating systems on which the command may be executed.
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * List of operating systems on which the command may be executed.
     * @return String
     * @since Ant 1.8.0
     */
    public final String getOs() {
        return os;
    }

    /**
     * Sets a command line.
     * @param cmdl command line.
     * @ant.attribute ignore="true"
     */
    public void setCommand(Commandline cmdl) {
        log("The command attribute is deprecated.\nPlease use the executable attribute and nested arg elements.",
            Project.MSG_WARN);
        this.cmdl = cmdl;
    }

    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output.
     *
     * @param out name of a file to which output should be sent.
     */
    public void setOutput(File out) {
        this.output = out;
        incompatibleWithSpawn = true;
    }

    /**
     * Set the input file to use for the task.
     *
     * @param input name of a file from which to get input.
     */
    public void setInput(File input) {
        if (inputString != null) {
            throw new BuildException(
                "The \"input\" and \"inputstring\" attributes cannot both be specified");
        }
        this.input = input;
        incompatibleWithSpawn = true;
    }

    /**
     * Set the string to use as input.
     *
     * @param inputString the string which is used as the input source.
     */
    public void setInputString(String inputString) {
        if (input != null) {
            throw new BuildException(
                "The \"input\" and \"inputstring\" attributes cannot both be specified");
        }
        this.inputString = inputString;
        incompatibleWithSpawn = true;
    }

    /**
     * Controls whether error output of exec is logged. This is only useful when
     * output is being redirected and error output is desired in the Ant log.
     *
     * @param logError set to true to log error output in the normal ant log.
     */
    public void setLogError(boolean logError) {
        redirector.setLogError(logError);
        incompatibleWithSpawn |= logError;
    }

    /**
     * Set the File to which the error stream of the process should be redirected.
     *
     * @param error a file to which stderr should be sent.
     *
     * @since Ant 1.6
     */
    public void setError(File error) {
        this.error = error;
        incompatibleWithSpawn = true;
    }

    /**
     * Sets the property name whose value should be set to the output of
     * the process.
     *
     * @param outputProp name of property.
     */
    public void setOutputproperty(String outputProp) {
        redirector.setOutputProperty(outputProp);
        incompatibleWithSpawn = true;
    }

    /**
     * Sets the name of the property whose value should be set to the error of
     * the process.
     *
     * @param errorProperty name of property.
     *
     * @since Ant 1.6
     */
    public void setErrorProperty(String errorProperty) {
        redirector.setErrorProperty(errorProperty);
        incompatibleWithSpawn = true;
    }

    /**
     * Fail if the command exits with a non-zero return code.
     *
     * @param fail if true fail the command on non-zero return code.
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
        incompatibleWithSpawn |= fail;
    }

    /**
     * Do not propagate old environment when new environment variables are specified.
     *
     * @param newenv if true, do not propagate old environment
     * when new environment variables are specified.
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Set whether to attempt to resolve the executable to a file.
     *
     * @param resolveExecutable if true, attempt to resolve the
     * path of the executable.
     */
    public void setResolveExecutable(boolean resolveExecutable) {
        this.resolveExecutable = resolveExecutable;
    }

    /**
     * Set whether to search nested, then
     * system PATH environment variables for the executable.
     *
     * @param searchPath if true, search PATHs.
     */
    public void setSearchPath(boolean searchPath) {
        this.searchPath = searchPath;
    }

    /**
     * Indicates whether to attempt to resolve the executable to a
     * file.
     * @return the resolveExecutable flag
     *
     * @since Ant 1.6
     */
    public boolean getResolveExecutable() {
        return resolveExecutable;
    }

    /**
     * Add an environment variable to the launched process.
     *
     * @param var new environment variable.
     */
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }

    /**
     * Adds a command-line argument.
     *
     * @return new command line argument created.
     */
    public Commandline.Argument createArg() {
        return cmdl.createArgument();
    }

    /**
     * Sets the name of a property in which the return code of the
     * command should be stored. Only of interest if failonerror=false.
     *
     * @since Ant 1.5
     *
     * @param resultProperty name of property.
     */
    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
        incompatibleWithSpawn = true;
    }

    /**
     * Helper method to set result property to the
     * passed in value if appropriate.
     *
     * @param result value desired for the result property value.
     */
    protected void maybeSetResultPropertyValue(int result) {
        if (resultProperty != null) {
            String res = Integer.toString(result);
            getProject().setNewProperty(resultProperty, res);
        }
    }

    /**
     * Set whether to stop the build if program cannot be started.
     * Defaults to true.
     *
     * @param flag stop the build if program cannot be started.
     *
     * @since Ant 1.5
     */
    public void setFailIfExecutionFails(boolean flag) {
        failIfExecFails = flag;
        incompatibleWithSpawn |= flag;
    }

    /**
     * Set whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     *
     * @param append if true append is desired.
     *
     * @since 1.30, Ant 1.5
     */
    public void setAppend(boolean append) {
        redirector.setAppend(append);
        incompatibleWithSpawn |= append;
    }

    /**
     * Whether output should be discarded.
     *
     * <p>Defaults to false.</p>
     *
     * @param discard
     *            if true output streams are discarded.
     *
     * @since Ant 1.10.10
     * @see #setDiscardError
     */
    public void setDiscardOutput(final boolean discard) {
        redirector.setDiscardOutput(discard);
    }

    /**
     * Whether error output should be discarded.
     *
     * <p>Defaults to false.</p>
     *
     * @param discard
     *            if true error streams are discarded.
     *
     * @since Ant 1.10.10
     * @see #setDiscardOutput
     */
    public void setDiscardError(final boolean discard) {
        redirector.setDiscardError(discard);
    }

    /**
     * Add a <code>RedirectorElement</code> to this task.
     *
     * @param redirectorElement   <code>RedirectorElement</code>.
     * @since Ant 1.6.2
     */
    public void addConfiguredRedirector(RedirectorElement redirectorElement) {
        if (this.redirectorElement != null) {
            throw new BuildException("cannot have > 1 nested <redirector>s");
        }
        this.redirectorElement = redirectorElement;
        incompatibleWithSpawn = true;
    }


    /**
     * Restrict this execution to a single OS Family
     * @param osFamily the family to restrict to.
     */
    public void setOsFamily(String osFamily) {
        this.osFamily = osFamily.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Restrict this execution to a single OS Family
     * @return the family to restrict to.
     * @since Ant 1.8.0
     */
    public final String getOsFamily() {
        return osFamily;
    }

    /**
     * The method attempts to figure out where the executable is so that we can feed
     * the full path. We first try basedir, then the exec dir, and then
     * fallback to the straight executable name (i.e. on the path).
     *
     * @param exec the name of the executable.
     * @param mustSearchPath if true, the executable will be looked up in
     * the PATH environment and the absolute path is returned.
     *
     * @return the executable as a full path if it can be determined.
     *
     * @since Ant 1.6
     */
    protected String resolveExecutable(String exec, boolean mustSearchPath) {
        if (!resolveExecutable) {
            return exec;
        }
        // try to find the executable
        File executableFile = getProject().resolveFile(exec);
        if (executableFile.exists()) {
            return executableFile.getAbsolutePath();
        }
        // now try to resolve against the dir if given
        if (dir != null) {
            executableFile = FILE_UTILS.resolveFile(dir, exec);
            if (executableFile.exists()) {
                return executableFile.getAbsolutePath();
            }
        }
        // couldn't find it - must be on path
        if (mustSearchPath) {
            Path p = null;
            String[] environment = env.getVariables();
            if (environment != null) {
                for (String variable : environment) {
                    if (isPath(variable)) {
                        p = new Path(getProject(), getPath(variable));
                        break;
                    }
                }
            }
            if (p == null) {
                String path = getPath(Execute.getEnvironmentVariables());
                if (path != null) {
                    p = new Path(getProject(), path);
                }
            }
            if (p != null) {
                for (String pathname : p.list()) {
                    executableFile
                            = FILE_UTILS.resolveFile(new File(pathname), exec);
                    if (executableFile.exists()) {
                        return executableFile.getAbsolutePath();
                    }
                }
            }
        }
        // mustSearchPath is false, or no PATH or not found - keep our
        // fingers crossed.
        return exec;
    }

    /**
     * Do the work.
     *
     * @throws BuildException in a number of circumstances:
     * <ul>
     * <li>if failIfExecFails is set to true and the process cannot be started</li>
     * <li>the java13command launcher can send build exceptions</li>
     * <li>this list is not exhaustive or limitative</li>
     * </ul>
     */
    @Override
    public void execute() throws BuildException {
        // Quick fail if this is not a valid OS for the command
        if (!isValidOs()) {
            return;
        }
        File savedDir = dir; // possibly altered in prepareExec
        cmdl.setExecutable(resolveExecutable(executable, searchPath));
        checkConfiguration();
        try {
            runExec(prepareExec());
        } finally {
            dir = savedDir;
        }
    }

    /**
     * Has the user set all necessary attributes?
     * @throws BuildException if there are missing required parameters.
     */
    protected void checkConfiguration() throws BuildException {
        if (cmdl.getExecutable() == null) {
            throw new BuildException("no executable specified", getLocation());
        }
        if (dir != null && !dir.exists()) {
            throw new BuildException("The directory " + dir + " does not exist");
        }
        if (dir != null && !dir.isDirectory()) {
            throw new BuildException(dir + " is not a directory");
        }
        if (spawn && incompatibleWithSpawn) {
            getProject().log("spawn does not allow attributes related to input, "
            + "output, error, result", Project.MSG_ERR);
            getProject().log("spawn also does not allow timeout", Project.MSG_ERR);
            getProject().log("finally, spawn is not compatible "
                + "with a nested I/O <redirector>", Project.MSG_ERR);
            throw new BuildException("You have used an attribute "
                + "or nested element which is not compatible with spawn");
        }
        setupRedirector();
    }

    /**
     * Set up properties on the redirector that we needed to store locally.
     */
    protected void setupRedirector() {
        redirector.setInput(input);
        redirector.setInputString(inputString);
        redirector.setOutput(output);
        redirector.setError(error);
    }

    /**
     * Is this the OS the user wanted?
     * @return boolean.
     * <ul>
     * <li>
     * <li><code>true</code> if the os and osfamily attributes are null.</li>
     * <li><code>true</code> if osfamily is set, and the os family and must match
     * that of the current OS, according to the logic of
     * {@link Os#isOs(String, String, String, String)}, and the result of the
     * <code>os</code> attribute must also evaluate true.
     * </li>
     * <li>
     * <code>true</code> if os is set, and the system.property os.name
     * is found in the os attribute,</li>
     * <li><code>false</code> otherwise.</li>
     * </ul>
     */
    protected boolean isValidOs() {
        //hand osfamily off to Os class, if set
        if (osFamily != null && !Os.isFamily(osFamily)) {
            return false;
        }
        //the Exec OS check is different from Os.isOs(), which
        //probes for a specific OS. Instead it searches the os field
        //for the current os.name
        String myos = System.getProperty("os.name");
        log("Current OS is " + myos, Project.MSG_VERBOSE);
        if (os != null && !os.contains(myos)) {
            // this command will be executed only on the specified OS
            log("This OS, " + myos
                    + " was not found in the specified list of valid OSes: " + os,
                    Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }

    /**
     * Set whether to launch new process with VM, otherwise use the OS's shell.
     * Default value is true.
     * @param vmLauncher true if we want to launch new process with VM,
     * false if we want to use the OS's shell.
     */
    public void setVMLauncher(boolean vmLauncher) {
        this.vmLauncher = vmLauncher;
    }

    /**
     * Create an Execute instance with the correct working directory set.
     *
     * @return an instance of the Execute class.
     *
     * @throws BuildException under unknown circumstances.
     */
    protected Execute prepareExec() throws BuildException {
        // default directory to the project's base directory
        if (dir == null) {
            dir = getProject().getBaseDir();
        }
        if (redirectorElement != null) {
            redirectorElement.configure(redirector);
        }
        Execute exe = new Execute(createHandler(), createWatchdog());
        exe.setAntRun(getProject());
        exe.setWorkingDirectory(dir);
        exe.setVMLauncher(vmLauncher);
        String[] environment = env.getVariables();
        if (environment != null) {
            for (String variable : environment) {
                log("Setting environment variable: " + variable,
                        Project.MSG_VERBOSE);
            }
        }
        exe.setNewenvironment(newEnvironment);
        exe.setEnvironment(environment);
        return exe;
    }

    /**
     * A Utility method for this classes and subclasses to run an
     * Execute instance (an external command).
     *
     * @param exe instance of the execute class.
     *
     * @throws IOException in case of problem to attach to the stdin/stdout/stderr
     * streams of the process.
     */
    protected final void runExecute(Execute exe) throws IOException {
        int returnCode = -1; // assume the worst

        if (!spawn) {
            returnCode = exe.execute();

            //test for and handle a forced process death
            if (exe.killedProcess()) {
                String msg = "Timeout: killed the sub-process";
                if (failOnError) {
                    throw new BuildException(msg);
                }
                log(msg, Project.MSG_WARN);
            }
            maybeSetResultPropertyValue(returnCode);
            redirector.complete();
            if (Execute.isFailure(returnCode)) {
                if (failOnError) {
                    throw new BuildException(getTaskType() + " returned: "
                        + returnCode, getLocation());
                }
                log("Result: " + returnCode, Project.MSG_ERR);
            }
        } else {
            exe.spawn();
        }
    }

    /**
     * Run the command using the given Execute instance. This may be
     * overridden by subclasses.
     *
     * @param exe instance of Execute to run.
     *
     * @throws BuildException if the new process could not be started
     * only if failIfExecFails is set to true (the default).
     */
    protected void runExec(Execute exe) throws BuildException {
        // show the command
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);

        exe.setCommandline(cmdl.getCommandline());
        try {
            runExecute(exe);
        } catch (IOException e) {
            if (failIfExecFails) {
                throw new BuildException("Execute failed: " + e.toString(), e,
                                         getLocation());
            }
            log("Execute failed: " + e.toString(), Project.MSG_ERR);
        } finally {
            // close the output file if required
            logFlush();
        }
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     *
     * @return instance of ExecuteStreamHandler.
     *
     * @throws BuildException under unknown circumstances.
     */
    protected ExecuteStreamHandler createHandler() throws BuildException {
        return redirector.createHandler();
    }

    /**
     * Create the Watchdog to kill a runaway process.
     *
     * @return instance of ExecuteWatchdog.
     *
     * @throws BuildException under unknown circumstances.
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        return (timeout == null)
            ? null : new ExecuteWatchdog(timeout);
    }

    /**
     * Flush the output stream - if there is one.
     */
    protected void logFlush() {
    }

    private boolean isPath(String line) {
        return line.startsWith("PATH=")
            || line.startsWith("Path=");
    }

    private String getPath(String line) {
        return line.substring("PATH=".length());
    }

    private String getPath(Map<String, String> map) {
        String p = map.get("PATH");
        return p != null ? p : map.get("Path");
    }
}
