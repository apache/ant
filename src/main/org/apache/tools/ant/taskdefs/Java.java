/*
 * Copyright  2000-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Assertions;
import org.apache.tools.ant.types.Permissions;

/**
 * Launcher for Java applications. Allows use of
 * the same JVM for the called application thus resulting in much
 * faster operation.
 *
 * @author Stefano Mazzocchi
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Stefan Bodewig
 * @author <a href="mailto:donal@savvion.com">Donal Quinlan</a>
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */
public class Java extends Task {

    private CommandlineJava cmdl = new CommandlineJava();
    private Environment env = new Environment();
    private boolean fork = false;
    private boolean newEnvironment = false;
    private File dir = null;
    private boolean failOnError = false;
    private Long timeout = null;
    private Redirector redirector = new Redirector(this);
    private String resultProperty;
    private Permissions perm = null;

    private boolean spawn = false;
    private boolean incompatibleWithSpawn = false;
    /**
     * Do the execution.
     * @throws BuildException if failOnError is set to true and the application
     * returns a non 0 result code
     */
    public void execute() throws BuildException {
        File savedDir = dir;
        Permissions savedPermissions = perm;

        int err = -1;
        try {
            err = executeJava();
            if (err != 0) {
                if (failOnError) {
                    throw new BuildException("Java returned: " + err, getLocation());
                } else {
                    log("Java Result: " + err, Project.MSG_ERR);
                }
            }
            maybeSetResultPropertyValue(err);
        } finally {
            dir = savedDir;
            perm = savedPermissions;
        }
    }

    /**
     * Do the execution and return a return code.
     *
     * @return the return code from the execute java class if it was
     * executed in a separate VM (fork = "yes").
     *
     * @throws BuildException if required parameters are missing
     */
    public int executeJava() throws BuildException {
        String classname = cmdl.getClassname();
        if (classname == null && cmdl.getJar() == null) {
            throw new BuildException("Classname must not be null.");
        }

        if (!fork && cmdl.getJar() != null) {
            throw new BuildException("Cannot execute a jar in non-forked mode."
                                     + " Please set fork='true'. ");
        }
        if (spawn && !fork) {
            throw new BuildException("Cannot spawn a java process in non-forked mode."
                                     + " Please set fork='true'. ");
        }
        if (spawn && incompatibleWithSpawn) {
            getProject().log("spawn does not allow attributes related to input, "
            + "output, error, result", Project.MSG_ERR);
            getProject().log("spawn does not also not allow timeout", Project.MSG_ERR);
            throw new BuildException("You have used an attribute which is "
            + "not compatible with spawn");
        }
        if (cmdl.getAssertions() != null && !fork) {
            log("Assertion statements are currently ignored in non-forked mode");
        }

        if (fork) {
            if (perm != null) {
                log("Permissions can not be set this way in forked mode.", Project.MSG_WARN);
            }
            log(cmdl.describeCommand(), Project.MSG_VERBOSE);
        } else {
            if (cmdl.getVmCommand().size() > 1) {
                log("JVM args ignored when same JVM is used.",
                    Project.MSG_WARN);
            }
            if (dir != null) {
                log("Working directory ignored when same JVM is used.",
                    Project.MSG_WARN);
            }

            if (newEnvironment || null != env.getVariables()) {
                log("Changes to environment variables are ignored when same "
                    + "JVM is used.", Project.MSG_WARN);
            }

            if (cmdl.getBootclasspath() != null) {
                log("bootclasspath ignored when same JVM is used.",
                    Project.MSG_WARN);
            }
            if (perm == null && failOnError == true) {
                perm = new Permissions(true);
                log("running " + this.cmdl.getClassname()
                    + " with default permissions (exit forbidden)", Project.MSG_VERBOSE);
            }
            log("Running in same VM " + cmdl.describeJavaCommand(),
                Project.MSG_VERBOSE);
        }

        try {
            if (fork) {
                if (!spawn) {
                    return fork(cmdl.getCommandline());
                } else {
                    spawn(cmdl.getCommandline());
                    return 0;
                }
            } else {
                try {
                    run(cmdl);
                    return 0;
                } catch (ExitException ex) {
                    return ex.getStatus();
                }
            }
        } catch (BuildException e) {
            if (failOnError) {
                throw e;
            } else {
                log(e.getMessage(), Project.MSG_ERR);
                return 0;
            }
        } catch (Throwable t) {
            if (failOnError) {
                throw new BuildException(t);
            } else {
                log(t.getMessage(), Project.MSG_ERR);
                return 0;
            }
        }
    }

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
     * Set the classpath to be used when running the Java class
     *
     * @param s an Ant Path object containing the classpath.
     */
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }

    /**
     * Adds a path to the classpath.
     *
     * @return created classpath
     */
    public Path createClasspath() {
        return cmdl.createClasspath(getProject()).createPath();
    }

    /**
     * Adds a path to the bootclasspath.
     * @since Ant 1.6
     *
     * @return created bootclasspath
     */
    public Path createBootclasspath() {
        return cmdl.createBootclasspath(getProject()).createPath();
    }

    /**
     * Sets the permissions for the application run inside the same JVM.
     * @since Ant 1.6
     * @return .
     */
    public Permissions createPermissions() {
        if (perm == null) {
            perm = new Permissions();
        }
        return perm;
    }

    /**
     * Classpath to use, by reference.
     *
     * @param r a reference to an existing classpath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * The location of the JAR file to execute.
     *
     * @param jarfile the jarfile that one wants to execute
     *
     * @throws BuildException if there is also a main class specified
     */
    public void setJar(File jarfile) throws BuildException {
        if (cmdl.getClassname() != null) {
            throw new BuildException("Cannot use 'jar' and 'classname' "
                                     + "attributes in same command.");
        }
        cmdl.setJar(jarfile.getAbsolutePath());
    }

    /**
     * Sets the Java class to execute.
     *
     * @param s the name of the main class
     *
     * @throws BuildException if the jar attribute has been set
     */
    public void setClassname(String s) throws BuildException {
        if (cmdl.getJar() != null) {
            throw new BuildException("Cannot use 'jar' and 'classname' "
                                     + "attributes in same command");
        }
        cmdl.setClassname(s);
    }

    /**
     * Deprecated: use nested arg instead.
     * Set the command line arguments for the class.
     *
     * @param s arguments
     *
     * @ant.attribute ignore="true"
     */
    public void setArgs(String s) {
        log("The args attribute is deprecated. "
            + "Please use nested arg elements.", Project.MSG_WARN);
        cmdl.createArgument().setLine(s);
    }

    /**
     * If set, system properties will be copied to the cloned VM - as
     * well as the bootclasspath unless you have explicitly specified
     * a bootclaspath.
     *
     * <p>Doesn't have any effect unless fork is true.</p>
     *
     * @since Ant 1.7
     */
    public void setCloneVm(boolean cloneVm) {
        cmdl.setCloneVm(cloneVm);
    }

    /**
     * Adds a command-line argument.
     *
     * @return created argument
     */
    public Commandline.Argument createArg() {
        return cmdl.createArgument();
    }

    /**
     * The name of a property in which the return code of the
     * command should be stored. Only of interest if failonerror=false.
     *
     * @param resultProperty name of property
     *
     * @since Ant 1.6
     */
    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
    }

    /**
     * helper method to set result property to the
     * passed in value if appropriate
     *
     * @param result the exit code
     */
    protected void maybeSetResultPropertyValue(int result) {
        String res = Integer.toString(result);
        if (resultProperty != null) {
            getProject().setNewProperty(resultProperty, res);
        }
    }

    /**
     * If true, execute in a new VM.
     *
     * @param s do you want to run Java in a new VM.
     */
    public void setFork(boolean s) {
        this.fork = s;
    }

    /**
     * Set the command line arguments for the JVM.
     *
     * @param s jvmargs
     */
    public void setJvmargs(String s) {
        log("The jvmargs attribute is deprecated. "
            + "Please use nested jvmarg elements.", Project.MSG_WARN);
        cmdl.createVmArgument().setLine(s);
    }

    /**
     * Adds a JVM argument.
     *
     * @return JVM argument created
     */
    public Commandline.Argument createJvmarg() {
        return cmdl.createVmArgument();
    }

    /**
     * Set the command used to start the VM (only if forking).
     *
     * @param s command to start the VM
     */
    public void setJvm(String s) {
        cmdl.setVm(s);
    }

    /**
     * Adds a system property.
     *
     * @param sysp system property
     */
    public void addSysproperty(Environment.Variable sysp) {
        cmdl.addSysproperty(sysp);
    }

    /**
     * Adds a set of properties as system properties.
     *
     * @param sysp set of properties to add
     *
     * @since Ant 1.6
     */
    public void addSyspropertyset(PropertySet sysp) {
        cmdl.addSyspropertyset(sysp);
    }

    /**
     * If true, then fail if the command exits with a
     * returncode other than 0
     *
     * @param fail if true fail the build when the command exits with a non
     * zero returncode
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
        incompatibleWithSpawn = true;
    }

    /**
     * The working directory of the process
     *
     * @param d working directory
     *
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * File the output of the process is redirected to.
     *
     * @param out name of the output file
     */
    public void setOutput(File out) {
        redirector.setOutput(out);
        incompatibleWithSpawn = true;
    }

    /**
     * Set the input to use for the task
     *
     * @param input name of the input file
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
     * @param logError get in the ant log the messages coming from stderr
     * in the case that fork = true
     */
    public void setLogError(boolean logError) {
        redirector.setLogError(logError);
        incompatibleWithSpawn = true;
    }

    /**
     * File the error stream of the process is redirected to.
     *
     * @param error file getting the error stream
     *
     * @since ant 1.6
     */
    public void setError(File error) {
        redirector.setError(error);
        incompatibleWithSpawn = true;
    }

    /**
     * Property name whose value should be set to the output of
     * the process.
     *
     * @param outputProp property name
     *
     */
    public void setOutputproperty(String outputProp) {
        redirector.setOutputProperty(outputProp);
        incompatibleWithSpawn = true;
    }

    /**
     * Property name whose value should be set to the error of
     * the process.
     *
     * @param errorProperty property name
     *
     * @since ant 1.6
     */
    public void setErrorProperty(String errorProperty) {
        redirector.setErrorProperty(errorProperty);
        incompatibleWithSpawn = true;
    }

    /**
     * Corresponds to -mx or -Xmx depending on VM version.
     *
     * @param max max memory parameter
     */
    public void setMaxmemory(String max) {
        cmdl.setMaxmemory(max);
    }

    /**
     * Sets the JVM version.
     * @param value JVM version
     */
    public void setJVMVersion(String value) {
        cmdl.setVmversion(value);
    }

    /**
     * Adds an environment variable.
     *
     * <p>Will be ignored if we are not forking a new VM.
     *
     * @param var new environment variable
     *
     * @since Ant 1.5
     */
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }

    /**
     * If true, use a completely new environment.
     *
     * <p>Will be ignored if we are not forking a new VM.
     *
     * @param newenv if true, use a completely new environment.
     *
     * @since Ant 1.5
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * If true, append output to existing file.
     *
     * @param append if true, append output to existing file
     *
     * @since Ant 1.5
     */
    public void setAppend(boolean append) {
        redirector.setAppend(append);
        incompatibleWithSpawn = true;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     *
     * @param value time out in milliseconds
     *
     * @since Ant 1.5
     */
    public void setTimeout(Long value) {
        timeout = value;
        incompatibleWithSpawn = true;
    }

    /**
     *  assertions to enable in this program (if fork=true)
     * @since Ant 1.6
     * @param asserts assertion set
     */
    public void addAssertions(Assertions asserts) {
        if(cmdl.getAssertions() != null) {
            throw new BuildException("Only one assertion declaration is allowed");
        }
        cmdl.setAssertions(asserts);
    }

    /**
     * Pass output sent to System.out to specified output file.
     *
     * @param output a string of output on its way to the handlers
     *
     * @since Ant 1.5
     */
    protected void handleOutput(String output) {
        if (redirector.getOutputStream() != null) {
            redirector.handleOutput(output);
        } else {
            super.handleOutput(output);
        }
    }

    /**
     * Handle an input request by this task
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read
     *
     * @return the number of bytes read
     *
     * @exception IOException if the data cannot be read
     * @since Ant 1.6
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (redirector.getInputStream() != null) {
            return redirector.handleInput(buffer, offset, length);
        } else {
            return super.handleInput(buffer, offset, length);
        }
    }

    /**
     * Pass output sent to System.out to specified output file.
     *
     * @param output string of output on its way to its handlers
     *
     * @since Ant 1.5.2
     */
    protected void handleFlush(String output) {
        if (redirector.getOutputStream() != null) {
            redirector.handleFlush(output);
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Handle output sent to System.err
     *
     * @param output string of stderr
     *
     * @since Ant 1.5
     */
    protected void handleErrorOutput(String output) {
        if (redirector.getErrorStream() != null) {
            redirector.handleErrorOutput(output);
        } else {
            super.handleErrorOutput(output);
        }
    }

    /**
     * Handle output sent to System.err and flush the stream.
     *
     * @param output string of stderr
     *
     * @since Ant 1.5.2
     */
    protected void handleErrorFlush(String output) {
        if (redirector.getErrorStream() != null) {
            redirector.handleErrorFlush(output);
        } else {
            super.handleErrorOutput(output);
        }
    }

    /**
     * Executes the given classname with the given arguments as it
     * was a command line application.
     */
    private void run(CommandlineJava command) throws BuildException {
        try {
            ExecuteJava exe = new ExecuteJava();
            exe.setJavaCommand(command.getJavaCommand());
            exe.setClasspath(command.getClasspath());
            exe.setSystemProperties(command.getSystemProperties());
            exe.setPermissions(perm);
            exe.setTimeout(timeout);
            redirector.createStreams();
            exe.execute(getProject());
            redirector.complete();
            if (exe.killedProcess()) {
                throw new BuildException("Timeout: killed the sub-process");
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     */
    private int fork(String[] command) throws BuildException {

            Execute exe
                = new Execute(redirector.createHandler(), createWatchdog());
            exe.setAntRun(getProject());

            if (dir == null) {
                dir = getProject().getBaseDir();
            } else if (!dir.exists() || !dir.isDirectory()) {
                throw new BuildException(dir.getAbsolutePath()
                                         + " is not a valid directory",
                                         getLocation());
            }

            exe.setWorkingDirectory(dir);

            String[] environment = env.getVariables();
            if (environment != null) {
                for (int i = 0; i < environment.length; i++) {
                    log("Setting environment variable: " + environment[i],
                        Project.MSG_VERBOSE);
                }
            }
            exe.setNewenvironment(newEnvironment);
            exe.setEnvironment(environment);

            exe.setCommandline(command);
            try {
                int rc = exe.execute();
                redirector.complete();
                if (exe.killedProcess()) {
                    throw new BuildException("Timeout: killed the sub-process");
                }
                return rc;
            } catch (IOException e) {
                throw new BuildException(e, getLocation());
            }
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     */
    private void spawn(String[] command) throws BuildException {

            Execute exe
                = new Execute();
            exe.setAntRun(getProject());

            if (dir == null) {
                dir = getProject().getBaseDir();
            } else if (!dir.exists() || !dir.isDirectory()) {
                throw new BuildException(dir.getAbsolutePath()
                                         + " is not a valid directory",
                                         getLocation());
            }

            exe.setWorkingDirectory(dir);

            String[] environment = env.getVariables();
            if (environment != null) {
                for (int i = 0; i < environment.length; i++) {
                    log("Setting environment variable: " + environment[i],
                        Project.MSG_VERBOSE);
                }
            }
            exe.setNewenvironment(newEnvironment);
            exe.setEnvironment(environment);

            exe.setCommandline(command);
            try {
                exe.spawn();
            } catch (IOException e) {
                throw new BuildException(e, getLocation());
            }
    }
    /**
     * Executes the given classname with the given arguments as it
     * was a command line application.
     *
     * @param classname the name of the class to run
     * @param args  arguments for the class
     * @throws BuildException in case of IO Exception in the execution
     */
    protected void run(String classname, Vector args) throws BuildException {
        CommandlineJava cmdj = new CommandlineJava();
        cmdj.setClassname(classname);
        for (int i = 0; i < args.size(); i++) {
            cmdj.createArgument().setValue((String) args.elementAt(i));
        }
        run(cmdj);
    }

    /**
     * Clear out the arguments to this java task.
     */
    public void clearArgs() {
        cmdl.clearJavaArgs();
    }

    /**
     * Create the Watchdog to kill a runaway process.
     *
     * @return new watchdog
     *
     * @throws BuildException under unknown circumstances
     *
     * @since Ant 1.5
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog(timeout.longValue());
    }

}
