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
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Assertions;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.KeepAliveInputStream;
import org.apache.tools.ant.util.StringUtils;

/**
 * Launcher for Java applications. Allows use of
 * the same JVM for the called application thus resulting in much
 * faster operation.
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */
public class Java extends Task {
    private static final String TIMEOUT_MESSAGE =
            "Timeout: killed the sub-process";

    private CommandlineJava cmdl = new CommandlineJava();
    private Environment env = new Environment();
    private boolean fork = false;
    private boolean newEnvironment = false;
    private File dir = null;
    private boolean failOnError = false;
    private Long timeout = null;

    //include locally for screening purposes
    private String inputString;
    private File input;
    private File output;
    private File error;

    // CheckStyle:VisibilityModifier OFF - bc
    protected Redirector redirector = new Redirector(this);
    protected RedirectorElement redirectorElement;
    // CheckStyle:VisibilityModifier ON

    private String resultProperty;
    private Permissions perm = null;

    private boolean spawn = false;
    private boolean incompatibleWithSpawn = false;

    /**
     * Normal constructor
     */
    public Java() {
    }

    /**
     * create a bound task
     * @param owner owner
     */
    public Java(Task owner) {
        bindToOwner(owner);
    }

    /**
     * Do the execution.
     * @throws BuildException if failOnError is set to true and the application
     * returns a nonzero result code.
     */
    @Override
    public void execute() throws BuildException {
        File savedDir = dir;
        Permissions savedPermissions = perm;

        int err = -1;
        try {
            checkConfiguration();
            err = executeJava();
            if (err != 0) {
                if (failOnError) {
                    throw new ExitStatusException("Java returned: " + err,
                            err,
                            getLocation());
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
     * executed in a separate VM (fork = "yes") or a security manager was
     * installed that prohibits ExitVM (default).
     *
     * @throws BuildException if required parameters are missing.
     */
    public int executeJava() throws BuildException {
        return executeJava(getCommandLine());
    }

    /**
     * Check configuration.
     * @throws BuildException if required parameters are missing.
     */
    protected void checkConfiguration() throws BuildException {
        String classname = getCommandLine().getClassname();
        String module = getCommandLine().getModule();
        final String sourceFile = getCommandLine().getSourceFile();
        if (classname == null && getCommandLine().getJar() == null && module == null && sourceFile == null) {
            throw new BuildException("Classname must not be null.");
        }
        if (!fork && getCommandLine().getJar() != null) {
            throw new BuildException(
                "Cannot execute a jar in non-forked mode. Please set fork='true'. ");
        }
        if (!fork && getCommandLine().getModule() != null) {
            throw new BuildException(
                "Cannot execute a module in non-forked mode. Please set fork='true'. ");
        }
        if (!fork && sourceFile != null) {
            throw new BuildException("Cannot execute sourcefile in non-forked mode. Please set fork='true'");
        }
        if (spawn && !fork) {
            throw new BuildException(
                "Cannot spawn a java process in non-forked mode. Please set fork='true'. ");
        }
        if (getCommandLine().getClasspath() != null
            && getCommandLine().getJar() != null) {
            log("When using 'jar' attribute classpath-settings are ignored. See the manual for more information.",
                Project.MSG_VERBOSE);
        }
        if (spawn && incompatibleWithSpawn) {
            getProject().log(
                "spawn does not allow attributes related to input, output, error, result",
                Project.MSG_ERR);
            getProject().log("spawn also does not allow timeout", Project.MSG_ERR);
            getProject().log(
                "finally, spawn is not compatible with a nested I/O <redirector>",
                Project.MSG_ERR);
            throw new BuildException(
                "You have used an attribute or nested element which is not compatible with spawn");
        }
        if (getCommandLine().getAssertions() != null && !fork) {
            log("Assertion statements are currently ignored in non-forked mode");
        }
        if (fork) {
            if (perm != null) {
                log("Permissions can not be set this way in forked mode.", Project.MSG_WARN);
            }
            log(getCommandLine().describeCommand(), Project.MSG_VERBOSE);
        } else {
            if (getCommandLine().getVmCommand().size() > 1) {
                log("JVM args ignored when same JVM is used.",
                    Project.MSG_WARN);
            }
            if (dir != null) {
                log("Working directory ignored when same JVM is used.",
                    Project.MSG_WARN);
            }
            if (newEnvironment || null != env.getVariables()) {
                log("Changes to environment variables are ignored when same JVM is used.",
                    Project.MSG_WARN);
            }
            if (getCommandLine().getBootclasspath() != null) {
                log("bootclasspath ignored when same JVM is used.",
                    Project.MSG_WARN);
            }
            if (perm == null) {
                perm = new Permissions(true);
                log("running " + this.getCommandLine().getClassname()
                    + " with default permissions (exit forbidden)", Project.MSG_VERBOSE);
            }
            log("Running in same VM " + getCommandLine().describeJavaCommand(),
                Project.MSG_VERBOSE);
        }
        setupRedirector();
    }

    /**
     * Execute the specified CommandlineJava.
     * @param commandLine CommandLineJava instance.
     * @return the exit value of the process if forked, 0 otherwise.
     */
    protected int executeJava(CommandlineJava commandLine) {
        try {
            if (fork) {
                if (spawn) {
                    spawn(commandLine.getCommandline());
                    return 0;
                }
                return fork(commandLine.getCommandline());
            }
            try {
                run(commandLine);
                return 0;
            } catch (ExitException ex) {
                return ex.getStatus();
            }
        } catch (BuildException e) {
            if (e.getLocation() == null && getLocation() != null) {
                e.setLocation(getLocation());
            }
            if (failOnError) {
                throw e;
            }
            if (TIMEOUT_MESSAGE.equals(e.getMessage())) {
                log(TIMEOUT_MESSAGE);
            } else {
                log(e);
            }
            return -1;
        } catch (ThreadDeath t) {
            throw t; // cf. NB #47191
        } catch (Throwable t) {
            if (failOnError) {
                throw new BuildException(t, getLocation());
            }
            log(t);
            return -1;
        }
    }

    /**
     * Set whether or not you want the process to be spawned;
     * default is not spawned.
     * @param spawn if true you do not want Ant to wait for the end of the process.
     * @since Ant 1.6
     */
    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    /**
     * Set the classpath to be used when running the Java class.
     *
     * @param s an Ant Path object containing the classpath.
     */
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }

    /**
     * Add a path to the classpath.
     *
     * @return created classpath.
     */
    public Path createClasspath() {
        return getCommandLine().createClasspath(getProject()).createPath();
    }

    /**
     * Add a path to the bootclasspath.
     * @since Ant 1.6
     *
     * @return created bootclasspath.
     */
    public Path createBootclasspath() {
        return getCommandLine().createBootclasspath(getProject()).createPath();
    }

    /**
     * Set the modulepath to be used when running the Java class.
     *
     * @param mp an Ant Path object containing the modulepath.
     * @since 1.9.7
     */
    public void setModulepath(Path mp) {
        createModulepath().append(mp);
    }

    /**
     * Add a path to the modulepath.
     *
     * @return created modulepath.
     * @since 1.9.7
     */
    public Path createModulepath() {
        return getCommandLine().createModulepath(getProject()).createPath();
    }

    /**
     * Set the modulepath to use by reference.
     *
     * @param r a reference to an existing modulepath.
     * @since 1.9.7
     */
    public void setModulepathRef(Reference r) {
        createModulepath().setRefid(r);
    }

    /**
     * Add a path to the upgrademodulepath.
     *
     * @return created upgrademodulepath.
     * @since 1.9.7
     */
    public Path createUpgrademodulepath() {
        return getCommandLine().createUpgrademodulepath(getProject()).createPath();
    }

    /**
     * Set the permissions for the application run inside the same JVM.
     * @since Ant 1.6
     * @return Permissions.
     */
    public Permissions createPermissions() {
        perm = (perm == null) ? new Permissions() : perm;
        return perm;
    }

    /**
     * Set the classpath to use by reference.
     *
     * @param r a reference to an existing classpath.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the location of the JAR file to execute.
     *
     * @param jarfile the jarfile to execute.
     *
     * @throws BuildException if there is also a {@code classname}, {@code module}
     *              or {@code sourcefile} attribute specified
     */
    public void setJar(File jarfile) throws BuildException {
        if (getCommandLine().getClassname() != null || getCommandLine().getModule() != null
                || getCommandLine().getSourceFile() != null) {
            throw new BuildException(
                    "Cannot use combination of 'jar', 'sourcefile', 'classname', 'module' attributes in same command");
        }
        getCommandLine().setJar(jarfile.getAbsolutePath());
    }

    /**
     * Set the Java class to execute.
     *
     * @param s the name of the main class.
     *
     * @throws BuildException if there is also a {@code jar} or {@code sourcefile} attribute specified
     */
    public void setClassname(String s) throws BuildException {
        if (getCommandLine().getJar() != null || getCommandLine().getSourceFile() != null) {
            throw new BuildException(
                "Cannot use combination of 'jar', 'classname', sourcefile attributes in same command");
        }
        getCommandLine().setClassname(s);
    }

    /**
     * Set the Java module to execute.
     *
     * @param module the name of the module.
     *
     * @throws BuildException if there is also a {@code jar} or {@code sourcefile} attribute specified
     * @since 1.9.7
     */
    public void setModule(String module) throws BuildException {
        if (getCommandLine().getJar() != null || getCommandLine().getSourceFile() != null) {
            throw new BuildException(
                    "Cannot use combination of 'jar', 'module', sourcefile attributes in same command");
        }
        getCommandLine().setModule(module);
    }

    /**
     * Set the Java source-file to execute. Support for single file source program
     * execution, in Java, is only available since Java 11.
     *
     * @param sourceFile The path to the source file
     * @throws BuildException if there is also a {@code jar}, {@code classname}
     *              or {@code module} attribute specified
     * @since Ant 1.10.5
     */
    public void setSourceFile(final String sourceFile) throws BuildException {
        final String jar = getCommandLine().getJar();
        final String className = getCommandLine().getClassname();
        final String module = getCommandLine().getModule();
        if (jar != null || className != null || module != null) {
            throw new BuildException("Cannot use 'sourcefile' in combination with 'jar' or " +
                    "'module' or 'classname'");
        }
        getCommandLine().setSourceFile(sourceFile);
    }

    /**
     * Deprecated: use nested arg instead.
     * Set the command line arguments for the class.
     *
     * @param s arguments.
     *
     * @ant.attribute ignore="true"
     */
    public void setArgs(String s) {
        log("The args attribute is deprecated. Please use nested arg elements.",
            Project.MSG_WARN);
        getCommandLine().createArgument().setLine(s);
    }

    /**
     * If set, system properties will be copied to the cloned VM--as
     * well as the bootclasspath unless you have explicitly specified
     * a bootclasspath.
     *
     * <p>Doesn't have any effect unless fork is true.</p>
     * @param cloneVm if true copy system properties.
     * @since Ant 1.7
     */
    public void setCloneVm(boolean cloneVm) {
        getCommandLine().setCloneVm(cloneVm);
    }

    /**
     * Add a command-line argument.
     *
     * @return created argument.
     */
    public Commandline.Argument createArg() {
        return getCommandLine().createArgument();
    }

    /**
     * Set the name of the property in which the return code of the
     * command should be stored. Only of interest if failonerror=false.
     *
     * @param resultProperty name of property.
     *
     * @since Ant 1.6
     */
    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
        incompatibleWithSpawn = true;
    }

    /**
     * Helper method to set result property to the
     * passed in value if appropriate.
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
     * @param s jvmargs.
     */
    public void setJvmargs(String s) {
        log("The jvmargs attribute is deprecated. Please use nested jvmarg elements.",
            Project.MSG_WARN);
        getCommandLine().createVmArgument().setLine(s);
    }

    /**
     * Adds a JVM argument.
     *
     * @return JVM argument created.
     */
    public Commandline.Argument createJvmarg() {
        return getCommandLine().createVmArgument();
    }

    /**
     * Set the command used to start the VM (only if forking).
     *
     * @param s command to start the VM.
     */
    public void setJvm(String s) {
        getCommandLine().setVm(s);
    }

    /**
     * Add a system property.
     *
     * @param sysp system property.
     */
    public void addSysproperty(Environment.Variable sysp) {
        getCommandLine().addSysproperty(sysp);
    }

    /**
     * Add a set of properties as system properties.
     *
     * @param sysp set of properties to add.
     *
     * @since Ant 1.6
     */
    public void addSyspropertyset(PropertySet sysp) {
        getCommandLine().addSyspropertyset(sysp);
    }

    /**
     * If true, then fail if the command exits with a
     * returncode other than zero.
     *
     * @param fail if true fail the build when the command exits with a
     * nonzero returncode.
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
        incompatibleWithSpawn |= fail;
    }

    /**
     * Set the working directory of the process.
     *
     * @param d working directory.
     *
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * Set the File to which the output of the process is redirected.
     *
     * @param out the output File.
     */
    public void setOutput(File out) {
        this.output = out;
        incompatibleWithSpawn = true;
    }

    /**
     * Set the input to use for the task.
     *
     * @param input name of the input file.
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
     * Set whether error output of exec is logged. This is only useful
     * when output is being redirected and error output is desired in the
     * Ant log.
     *
     * @param logError get in the ant log the messages coming from stderr
     * in the case that fork = true.
     */
    public void setLogError(boolean logError) {
        redirector.setLogError(logError);
        incompatibleWithSpawn |= logError;
    }

    /**
     * Set the File to which the error stream of the process is redirected.
     *
     * @param error file getting the error stream.
     *
     * @since Ant 1.6
     */
    public void setError(File error) {
        this.error = error;
        incompatibleWithSpawn = true;
    }

    /**
     * Set the property name whose value should be set to the output of
     * the process.
     *
     * @param outputProp property name.
     *
     */
    public void setOutputproperty(String outputProp) {
        redirector.setOutputProperty(outputProp);
        incompatibleWithSpawn = true;
    }

    /**
     * Set the property name whose value should be set to the error of
     * the process.
     *
     * @param errorProperty property name.
     *
     * @since Ant 1.6
     */
    public void setErrorProperty(String errorProperty) {
        redirector.setErrorProperty(errorProperty);
        incompatibleWithSpawn = true;
    }

    /**
     * Corresponds to -mx or -Xmx depending on VM version.
     *
     * @param max max memory parameter.
     */
    public void setMaxmemory(String max) {
        getCommandLine().setMaxmemory(max);
    }

    /**
     * Set the JVM version.
     * @param value JVM version.
     */
    public void setJVMVersion(String value) {
        getCommandLine().setVmversion(value);
    }

    /**
     * Add an environment variable.
     *
     * <p>Will be ignored if we are not forking a new VM.
     *
     * @param var new environment variable.
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
     * @param append if true, append output to existing file.
     *
     * @since Ant 1.5
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
     * Add assertions to enable in this program (if fork=true).
     * @param asserts assertion set.
     * @since Ant 1.6
     */
    public void addAssertions(Assertions asserts) {
        if (getCommandLine().getAssertions() != null) {
            throw new BuildException("Only one assertion declaration is allowed");
        }
        getCommandLine().setAssertions(asserts);
    }

    /**
     * Add a <code>RedirectorElement</code> to this task.
     * @param redirectorElement   <code>RedirectorElement</code>.
     */
    public void addConfiguredRedirector(RedirectorElement redirectorElement) {
        if (this.redirectorElement != null) {
            throw new BuildException("cannot have > 1 nested redirectors");
        }
        this.redirectorElement = redirectorElement;
        incompatibleWithSpawn = true;
    }

    /**
     * Pass output sent to System.out to specified output file.
     *
     * @param output a string of output on its way to the handlers.
     *
     * @since Ant 1.5
     */
    @Override
    protected void handleOutput(String output) {
        if (redirector.getOutputStream() != null) {
            redirector.handleOutput(output);
        } else {
            super.handleOutput(output);
        }
    }

    /**
     * Handle an input request by this task.
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @since Ant 1.6
     */
    @Override
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        // Should work whether or not redirector.inputStream == null:
        return redirector.handleInput(buffer, offset, length);
    }

    /**
     * Pass output sent to System.out to specified output file.
     *
     * @param output string of output on its way to its handlers.
     *
     * @since Ant 1.5.2
     */
    @Override
    protected void handleFlush(String output) {
        if (redirector.getOutputStream() != null) {
            redirector.handleFlush(output);
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Handle output sent to System.err.
     *
     * @param output string of stderr.
     *
     * @since Ant 1.5
     */
    @Override
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
     * @param output string of stderr.
     *
     * @since Ant 1.5.2
     */
    @Override
    protected void handleErrorFlush(String output) {
        if (redirector.getErrorStream() != null) {
            redirector.handleErrorFlush(output);
        } else {
            super.handleErrorFlush(output);
        }
    }

    /**
     * Set up properties on the redirector that we needed to store locally.
     */
    protected void setupRedirector() {
        redirector.setInput(input);
        redirector.setInputString(inputString);
        redirector.setOutput(output);
        redirector.setError(error);
        if (redirectorElement != null) {
            redirectorElement.configure(redirector);
        }
        if (!spawn && input == null && inputString == null) {
            // #24918: send standard input to the process by default.
            redirector.setInputStream(
                new KeepAliveInputStream(getProject().getDefaultInputStream()));
        }
    }

    /**
     * Executes the given classname with the given arguments as it
     * were a command line application.
     * @param command CommandlineJava.
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
                throw new BuildException(TIMEOUT_MESSAGE);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Executes the given source-file or classname with the given arguments in a separate VM.
     * @param command String[] of command-line arguments.
     */
    private int fork(String[] command) throws BuildException {
        Execute exe
            = new Execute(redirector.createHandler(), createWatchdog());
        setupExecutable(exe, command);

        try {
            int rc = exe.execute();
            redirector.complete();
            if (exe.killedProcess()) {
                throw new BuildException(TIMEOUT_MESSAGE);
            }
            return rc;
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     * @param command String[] of command-line arguments.
     */
    private void spawn(String[] command) throws BuildException {
        Execute exe = new Execute();
        setupExecutable(exe, command);
        try {
            exe.spawn();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    /**
     * Do all configuration for an executable that
     * is common across the {@link #fork(String[])} and
     * {@link #spawn(String[])} methods.
     * @param exe executable.
     * @param command command to execute.
     */
    private void setupExecutable(Execute exe, String[] command) {
        exe.setAntRun(getProject());
        setupWorkingDir(exe);
        setupEnvironment(exe);
        setupCommandLine(exe, command);
    }

    /**
     * Set up our environment variables.
     * @param exe executable.
     */
    private void setupEnvironment(Execute exe) {
        String[] environment = env.getVariables();
        if (environment != null) {
            for (String element : environment) {
                log("Setting environment variable: " + element,
                    Project.MSG_VERBOSE);
            }
        }
        exe.setNewenvironment(newEnvironment);
        exe.setEnvironment(environment);
    }

    /**
     * Set the working dir of the new process.
     * @param exe executable.
     * @throws BuildException if the dir doesn't exist.
     */
    private void setupWorkingDir(Execute exe) {
        if (dir == null) {
            dir = getProject().getBaseDir();
        } else if (!dir.isDirectory()) {
            throw new BuildException(dir.getAbsolutePath()
                                     + " is not a valid directory",
                                     getLocation());
        }
        exe.setWorkingDirectory(dir);
    }

    /**
     * Set the command line for the exe.
     * On VMS, hands off to {@link #setupCommandLineForVMS(Execute, String[])}.
     * @param exe executable.
     * @param command command to execute.
     */
    private void setupCommandLine(Execute exe, String[] command) {
        //On VMS platform, we need to create a special java options file
        //containing the arguments and classpath for the java command.
        //The special file is supported by the "-V" switch on the VMS JVM.
        if (Os.isFamily("openvms")) {
            setupCommandLineForVMS(exe, command);
        } else {
            exe.setCommandline(command);
        }
    }

    /**
     * On VMS platform, we need to create a special java options file
     * containing the arguments and classpath for the java command.
     * The special file is supported by the "-V" switch on the VMS JVM.
     *
     * @param exe executable.
     * @param command command to execute.
     */
    private void setupCommandLineForVMS(Execute exe, String[] command) {
        ExecuteJava.setupCommandLineForVMS(exe, command);
    }

    /**
     * Executes the given classname with the given arguments as if it
     * were a command line application.
     *
     * @param classname the name of the class to run.
     * @param args  arguments for the class.
     * @throws BuildException in case of IOException in the execution.
     */
    protected void run(String classname, Vector<String> args) throws BuildException {
        CommandlineJava cmdj = new CommandlineJava();
        cmdj.setClassname(classname);
        args.forEach(arg -> cmdj.createArgument().setValue(arg));
        run(cmdj);
    }

    /**
     * Clear out the arguments to this java task.
     */
    public void clearArgs() {
        getCommandLine().clearJavaArgs();
    }

    /**
     * Create the Watchdog to kill a runaway process.
     *
     * @return new watchdog.
     *
     * @throws BuildException under unknown circumstances.
     *
     * @since Ant 1.5
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog(timeout);
    }

    /**
     * Log the specified Throwable.
     * @param t the Throwable to log.
     * @since 1.6.2
     */
    private void log(Throwable t) {
        log(StringUtils.getStackTrace(t), Project.MSG_ERR);
    }

    /**
     * Accessor to the command line.
     *
     * @return the current command line.
     * @since 1.6.3
     */
    public CommandlineJava getCommandLine() {
        return cmdl;
    }

    /**
     * Get the system properties of the command line.
     *
     * @return the current properties of this java invocation.
     * @since 1.6.3
     */
    public CommandlineJava.SysProperties getSysProperties() {
        return getCommandLine().getSystemProperties();
    }


}
