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

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.util.FileUtils;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * @author duncan@x180.com
 * @author rubys@us.ibm.com
 * @author thomas.haas@softwired-inc.com
 * @author Stefan Bodewig
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 * @author <a href="mailto:CHudak@arrowheadgrp.com">Charles Hudak</a>
 *
 * @since Ant 1.2
 *
 * @ant.task category="control"
 */
public class ExecTask extends Task {

    private String os;

    private File dir;
    protected boolean failOnError = false;
    protected boolean newEnvironment = false;
    private Long timeout = null;
    private Environment env = new Environment();
    protected Commandline cmdl = new Commandline();
    private String resultProperty;
    private boolean failIfExecFails = true;
    private String executable;
    private boolean resolveExecutable = false;
    private boolean spawn = false;
    private boolean incompatibleWithSpawn = false;

    private Redirector redirector = new Redirector(this);

    /**
     * Controls whether the VM (1.3 and above) is used to execute the
     * command
     */
    private boolean vmLauncher = true;

    /**
     * set whether or not you want the process to be spawned
     * default is not spawned
     * @param spawn if true you do not want ant to wait for the end of the process
     * @since ant 1.6
     */
    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     *
     * @param value timeout in milliseconds
     *
     * @since Ant 1.5
     */
    public void setTimeout(Long value) {
        timeout = value;
        incompatibleWithSpawn = true;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     *
     * @param value timeout in milliseconds
     */
    public void setTimeout(Integer value) {
        if (value == null) {
            timeout = null;
        } else {
            setTimeout(new Long(value.intValue()));
        }
        incompatibleWithSpawn = true;
    }

    /**
     * Set the name of the executable program.
     * @param value the name of the executable program
     */
    public void setExecutable(String value) {
        this.executable = value;
        cmdl.setExecutable(value);
    }

    /**
     * Set the working directory of the process.
     * @param d the working directory of the process
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * List of operating systems on which the command may be executed.
     * @param os list of operating systems on which the command may be executed
     */
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * Sets a command line
     * @param cmdl command line
     * @ant.attribute ignore="true"
     */
    public void setCommand(Commandline cmdl) {
        log("The command attribute is deprecated. "
            + "Please use the executable attribute and nested arg elements.",
            Project.MSG_WARN);
        this.cmdl = cmdl;
    }

    /**
     * File the output of the process is redirected to. If error is not
     * redirected, it too will appear in the output
     *
     * @param out name of a file to which send output to
     */
    public void setOutput(File out) {
        redirector.setOutput(out);
        incompatibleWithSpawn = true;
    }

    /**
     * Set the input to use for the task
     *
     * @param input name of a file to get input from
     */
    public void setInput(File input) {
        redirector.setInput(input);
        incompatibleWithSpawn = true;
    }

    /**
     * Set the string to use as input
     *
     * @param inputString the string which is used as the input source
     */
    public void setInputString(String inputString) {
        redirector.setInputString(inputString);
        incompatibleWithSpawn = true;
    }

    /**
     * Controls whether error output of exec is logged. This is only useful
     * when output is being redirected and error output is desired in the
     * Ant log
     *
     * @param logError set to true to log error output in the normal ant log
     */
    public void setLogError(boolean logError) {
        redirector.setLogError(logError);
        incompatibleWithSpawn = true;
    }

    /**
     * File the error stream of the process is redirected to.
     *
     * @param error a file to which send stderr to
     *
     * @since ant 1.6
     */
    public void setError(File error) {
        redirector.setError(error);
        incompatibleWithSpawn = true;
    }

    /**
     * Sets the property name whose value should be set to the output of
     * the process.
     *
     * @param outputProp name of property
     */
    public void setOutputproperty(String outputProp) {
        redirector.setOutputProperty(outputProp);
        incompatibleWithSpawn = true;
    }

    /**
     * Sets the name of the property whose value should be set to the error of
     * the process.
     *
     * @param errorProperty name of property
     *
     * @since ant 1.6
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
        incompatibleWithSpawn = true;
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
     * Sets a flag indicating whether to attempt to resolve the executable
     * to a file
     *
     * @param resolveExecutable if true, attempt to resolve the
     * path of the executable
     */
    public void setResolveExecutable(boolean resolveExecutable) {
        this.resolveExecutable = resolveExecutable;
    }

    /**
     * Add an environment variable to the launched process.
     *
     * @param var new environment variable
     */
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }

    /**
     * Adds a command-line argument.
     *
     * @return new command line argument created
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
     * @param resultProperty name of property
     */
    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
        incompatibleWithSpawn = true;
    }

    /**
     * helper method to set result property to the
     * passed in value if appropriate
     *
     * @param result value desired for the result property value
     */
    protected void maybeSetResultPropertyValue(int result) {
        String res = Integer.toString(result);
        if (resultProperty != null) {
            getProject().setNewProperty(resultProperty, res);
        }
    }

    /**
     * Sets a flag to stop the build if program cannot be started.
     * Defaults to true.
     *
     * @param flag stop the build if program cannot be started
     *
     * @since Ant 1.5
     */
    public void setFailIfExecutionFails(boolean flag) {
        failIfExecFails = flag;
        incompatibleWithSpawn = true;
    }

    /**
     * Sets whether output should be appended to or overwrite an existing file.
     * Defaults to false.
     *
     * @param append if true append is desired
     *
     * @since 1.30, Ant 1.5
     */
    public void setAppend(boolean append) {
        redirector.setAppend(append);
        incompatibleWithSpawn = true;
    }



    /**
     * Attempt to figure out where the executable is so that we can feed
     * the full path - first try basedir, then the exec dir and then
     * fallback to the straight executable name (i.e. on ther path)
     *
     * @return the executable as a full path if it can be determined.
     */
    private String resolveExecutable() {
        if (!resolveExecutable) {
            return executable;
        }

        // try to find the executable
        File executableFile = getProject().resolveFile(executable);
        if (executableFile.exists()) {
            return executableFile.getAbsolutePath();
        }

        // now try to resolve against the dir if given
        if (dir != null) {
            FileUtils fileUtils = FileUtils.newFileUtils();
            executableFile = fileUtils.resolveFile(dir, executable);
            if (executableFile.exists()) {
                return executableFile.getAbsolutePath();
            }
        }

        // couldn't find it - must be on path
        return executable;
    }

    /**
     * Do the work.
     *
     * @throws BuildException in a number of circumstances :
     * <ul>
     * <li>if failIfExecFails is set to true and the process cannot be started</li>
     * <li>the java13command launcher can send build exceptions</li>
     * <li>this list is not exhaustive or limitative</li>
     * </ul>
     */
    public void execute() throws BuildException {
        File savedDir = dir; // possibly altered in prepareExec
        cmdl.setExecutable(resolveExecutable());
        checkConfiguration();
        if (isValidOs()) {
            try {
                runExec(prepareExec());
            } finally {
                dir = savedDir;
            }
        }
    }

    /**
     * Has the user set all necessary attributes?
     * @throws BuildException if there are missing required parameters
     */
    protected void checkConfiguration() throws BuildException {
        if (cmdl.getExecutable() == null) {
            throw new BuildException("no executable specified", getLocation());
        }
        if (dir != null && !dir.exists()) {
            throw new BuildException("The directory you specified does not "
                                     + "exist");
        }
        if (dir != null && !dir.isDirectory()) {
            throw new BuildException("The directory you specified is not a "
                                     + "directory");
        }
        if (spawn && incompatibleWithSpawn) {
            getProject().log("spawn does not allow attributes related to input, "
            + "output, error, result", Project.MSG_ERR);
            getProject().log("spawn does not also not allow timeout", Project.MSG_ERR);
            throw new BuildException("You have used an attribute which is "
            + "not compatible with spawn");
        }
    }

    /**
     * Is this the OS the user wanted?
     * @return boolean
     * <ul>
     * <li>
     * <code>true</code> if the os under which ant is running is
     * matches one os in the os attribute
     * or if the os attribute is null</li>
     * <li><code>false</code> otherwise.</li>
     * </ul>
     */
    protected boolean isValidOs() {
        // test if os match
        String myos = System.getProperty("os.name");
        log("Current OS is " + myos, Project.MSG_VERBOSE);
        if ((os != null) && (os.indexOf(myos) < 0)) {
            // this command will be executed only on the specified OS
            log("This OS, " + myos
                + " was not found in the specified list of valid OSes: " + os,
                Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }

    /**
     * Sets a flag indicating if we want to launch new process with VM,
     * otherwise use the OS's shell.
     * Default value of the flag is true.
     * @param vmLauncher true if we want to launch new process with VM,
     * false if we want to use the OS's shell.
     */
    public void setVMLauncher(boolean vmLauncher) {
        this.vmLauncher = vmLauncher;
    }

    /**
     * Create an Execute instance with the correct working directory set.
     *
     * @return an instance of the Execute class
     *
     * @throws BuildException under unknown circumstances.
     */
    protected Execute prepareExec() throws BuildException {
        // default directory to the project's base directory
        if (dir == null) {
            dir = getProject().getBaseDir();
        }
        Execute exe = new Execute(createHandler(), createWatchdog());
        exe.setAntRun(getProject());
        exe.setWorkingDirectory(dir);
        exe.setVMLauncher(vmLauncher);
        exe.setSpawn(spawn);
        String[] environment = env.getVariables();
        if (environment != null) {
            for (int i = 0; i < environment.length; i++) {
                log("Setting environment variable: " + environment[i],
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
     * @param exe instance of the execute class
     *
     * @throws IOException in case of problem to attach to the stdin/stdout/stderr
     * streams of the process
     */
    protected final void runExecute(Execute exe) throws IOException {
        int returnCode = -1; // assume the worst

        if (!spawn) {
            returnCode = exe.execute();

            //test for and handle a forced process death
            if (exe.killedProcess()) {
                log("Timeout: killed the sub-process", Project.MSG_WARN);
            }
            maybeSetResultPropertyValue(returnCode);
            if (Execute.isFailure(returnCode)) {
                if (failOnError) {
                    throw new BuildException(getTaskType() + " returned: "
                        + returnCode, getLocation());
                } else {
                    log("Result: " + returnCode, Project.MSG_ERR);
                }
            }
            redirector.complete();
        } else {
            exe.spawn();
        }
    }

    /**
     * Run the command using the given Execute instance. This may be
     * overidden by subclasses
     *
     * @param exe instance of Execute to run
     *
     * @throws BuildException if the new process could not be started
     * only if failIfExecFails is set to true (the default)
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
            } else {
                log("Execute failed: " + e.toString(), Project.MSG_ERR);
            }
        } finally {
            // close the output file if required
            logFlush();
        }
    }

    /**
     * Create the StreamHandler to use with our Execute instance.
     *
     * @return instance of ExecuteStreamHandler
     *
     * @throws BuildException under unknown circumstances
     */
    protected ExecuteStreamHandler createHandler() throws BuildException {
        return redirector.createHandler();
    }

    /**
     * Create the Watchdog to kill a runaway process.
     *
     * @return instance of ExecuteWatchdog
     *
     * @throws BuildException under unknown circumstances
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog(timeout.longValue());
    }

    /**
     * Flush the output stream - if there is one.
     */
    protected void logFlush() {
    }

}
