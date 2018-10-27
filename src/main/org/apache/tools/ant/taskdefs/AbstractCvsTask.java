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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * original Cvs.java 1.20
 *
 *  NOTE: This implementation has been moved here from Cvs.java with
 *  the addition of some accessors for extensibility.  Another task
 *  can extend this with some customized output processing.
 *
 * @since Ant 1.5
 */
public abstract class AbstractCvsTask extends Task {
    /**
     * Default compression level to use, if compression is enabled via
     * setCompression( true ).
     */
    public static final int DEFAULT_COMPRESSION_LEVEL = 3;
    private static final int MAXIMUM_COMRESSION_LEVEL = 9;

    private Commandline cmd = new Commandline();

    private List<Module> modules = new ArrayList<>();

    /** list of Commandline children */
    private List<Commandline> commandlines = new Vector<>();

    /**
     * the CVSROOT variable.
     */
    private String cvsRoot;

    /**
     * the CVS_RSH variable.
     */
    private String cvsRsh;

    /**
     * the package/module to check out.
     */
    private String cvsPackage;
    /**
     * the tag
     */
    private String tag;
    /**
     * the default command.
     */
    private static final String DEFAULT_COMMAND = "checkout";
    /**
     * the CVS command to execute.
     */
    private String command = null;

    /**
     * suppress information messages.
     */
    private boolean quiet = false;

    /**
     * suppress all messages.
     */
    private boolean reallyquiet = false;

    /**
     * compression level to use.
     */
    private int compression = 0;

    /**
     * report only, don't change any files.
     */
    private boolean noexec = false;

    /**
     * CVS port
     */
    private int port = 0;

    /**
     * CVS password file
     */
    private File passFile = null;

    /**
     * the directory where the checked out files should be placed.
     */
    private File dest;

    /** whether or not to append stdout/stderr to existing files */
    private boolean append = false;

    /**
     * the file to direct standard output from the command.
     */
    private File output;

    /**
     * the file to direct standard error from the command.
     */
    private File error;

    /**
     * If true it will stop the build if cvs exits with error.
     * Default is false. (Iulian)
     */
    private boolean failOnError = false;

    /**
     * Create accessors for the following, to allow different handling of
     * the output.
     */
    private ExecuteStreamHandler executeStreamHandler;
    private OutputStream outputStream;
    private OutputStream errorStream;

    /**
     * sets the handler
     * @param handler a handler able of processing the output and error streams from the cvs exe
     */
    public void setExecuteStreamHandler(ExecuteStreamHandler handler) {
        this.executeStreamHandler = handler;
    }

    /**
     * find the handler and instantiate it if it does not exist yet
     * @return handler for output and error streams
     */
    protected ExecuteStreamHandler getExecuteStreamHandler() {

        if (this.executeStreamHandler == null) {
            setExecuteStreamHandler(new PumpStreamHandler(getOutputStream(),
                                                          getErrorStream()));
        }

        return this.executeStreamHandler;
    }

    /**
     * sets a stream to which the output from the cvs executable should be sent
     * @param outputStream stream to which the stdout from cvs should go
     */
    protected void setOutputStream(OutputStream outputStream) {

        this.outputStream = outputStream;
    }

    /**
     * access the stream to which the stdout from cvs should go
     * if this stream has already been set, it will be returned
     * if the stream has not yet been set, if the attribute output
     * has been set, the output stream will go to the output file
     * otherwise the output will go to ant's logging system
     * @return output stream to which cvs' stdout should go to
     */
    protected OutputStream getOutputStream() {

        if (this.outputStream == null) {

            if (output != null) {
                try {
                    setOutputStream(new PrintStream(
                                        new BufferedOutputStream(
                                            FileUtils.newOutputStream(Paths.get(output.getPath()),
                                                                 append))));
                } catch (IOException e) {
                    throw new BuildException(e, getLocation());
                }
            } else {
                setOutputStream(new LogOutputStream(this, Project.MSG_INFO));
            }
        }

        return this.outputStream;
    }

    /**
     * sets a stream to which the stderr from the cvs exe should go
     * @param errorStream an output stream willing to process stderr
     */
    protected void setErrorStream(OutputStream errorStream) {

        this.errorStream = errorStream;
    }

    /**
     * access the stream to which the stderr from cvs should go
     * if this stream has already been set, it will be returned
     * if the stream has not yet been set, if the attribute error
     * has been set, the output stream will go to the file denoted by the error attribute
     * otherwise the stderr output will go to ant's logging system
     * @return output stream to which cvs' stderr should go to
     */
    protected OutputStream getErrorStream() {

        if (this.errorStream == null) {

            if (error != null) {

                try {
                    setErrorStream(new PrintStream(
                                       new BufferedOutputStream(
                                           FileUtils.newOutputStream(Paths.get(error.getPath()),
                                                                append))));
                } catch (IOException e) {
                    throw new BuildException(e, getLocation());
                }
            } else {
                setErrorStream(new LogOutputStream(this, Project.MSG_WARN));
            }
        }

        return this.errorStream;
    }

    /**
     * Sets up the environment for toExecute and then runs it.
     * @param toExecute the command line to execute
     * @throws BuildException if failonError is set to true and the cvs command fails
     */
    protected void runCommand(Commandline toExecute) throws BuildException {
        // TODO: we should use JCVS (www.ice.com/JCVS) instead of
        // command line execution so that we don't rely on having
        // native CVS stuff around (SM)

        // We can't do it ourselves as jCVS is GPLed, a third party task
        // outside of Apache repositories would be possible though (SB).

        Environment env = new Environment();

        if (port > 0) {
            Environment.Variable var = new Environment.Variable();
            var.setKey("CVS_CLIENT_PORT");
            var.setValue(String.valueOf(port));
            env.addVariable(var);

            // non-standard environment variable used by CVSNT, WinCVS
            // and others
            var = new Environment.Variable();
            var.setKey("CVS_PSERVER_PORT");
            var.setValue(String.valueOf(port));
            env.addVariable(var);
        }

        /**
         * Need a better cross platform integration with <cvspass>, so
         * use the same filename.
         */
        if (passFile == null) {

            File defaultPassFile = new File(
                System.getProperty("cygwin.user.home",
                    System.getProperty("user.home"))
                + File.separatorChar + ".cvspass");

            if (defaultPassFile.exists()) {
                this.setPassfile(defaultPassFile);
            }
        }

        if (passFile != null) {
            if (passFile.isFile() && passFile.canRead()) {
                Environment.Variable var = new Environment.Variable();
                var.setKey("CVS_PASSFILE");
                var.setValue(String.valueOf(passFile));
                env.addVariable(var);
                log("Using cvs passfile: " + String.valueOf(passFile),
                    Project.MSG_VERBOSE);
            } else if (!passFile.canRead()) {
                log("cvs passfile: " + String.valueOf(passFile)
                    + " ignored as it is not readable",
                    Project.MSG_WARN);
            } else {
                log("cvs passfile: " + String.valueOf(passFile)
                    + " ignored as it is not a file",
                    Project.MSG_WARN);
            }
        }

        if (cvsRsh != null) {
            Environment.Variable var = new Environment.Variable();
            var.setKey("CVS_RSH");
            var.setValue(String.valueOf(cvsRsh));
            env.addVariable(var);
        }

        //
        // Just call the getExecuteStreamHandler() and let it handle
        //     the semantics of instantiation or retrieval.
        //
        Execute exe = new Execute(getExecuteStreamHandler(), null);

        exe.setAntRun(getProject());
        if (dest == null) {
            dest = getProject().getBaseDir();
        }

        if (!dest.exists()) {
            dest.mkdirs();
        }

        exe.setWorkingDirectory(dest);
        exe.setCommandline(toExecute.getCommandline());
        exe.setEnvironment(env.getVariables());

        try {
            String actualCommandLine = executeToString(exe);

            log(actualCommandLine, Project.MSG_VERBOSE);
            int retCode = exe.execute();
            log("retCode=" + retCode, Project.MSG_DEBUG);

            if (failOnError && Execute.isFailure(retCode)) {
                throw new BuildException("cvs exited with error code "
                                         + retCode
                                         + StringUtils.LINE_SEP
                                         + "Command line was ["
                                         + actualCommandLine + "]",
                                         getLocation());
            }
        } catch (IOException e) {
            if (failOnError) {
                throw new BuildException(e, getLocation());
            }
            log("Caught exception: " + e.getMessage(), Project.MSG_WARN);
        } catch (BuildException e) {
            if (failOnError) {
                throw(e);
            }
            Throwable t = e.getCause();
            if (t == null) {
                t = e;
            }
            log("Caught exception: " + t.getMessage(), Project.MSG_WARN);
        } catch (Exception e) {
            if (failOnError) {
                throw new BuildException(e, getLocation());
            }
            log("Caught exception: " + e.getMessage(), Project.MSG_WARN);
        }
    }

    /**
     * do the work
     * @throws BuildException if failonerror is set to true and the
     * cvs command fails.
     */
    @Override
    public void execute() throws BuildException {

        String savedCommand = getCommand();

        if (this.getCommand() == null && commandlines.isEmpty()) {
            // re-implement legacy behaviour:
            this.setCommand(AbstractCvsTask.DEFAULT_COMMAND);
        }

        String c = this.getCommand();
        Commandline cloned = null;
        if (c != null) {
            cloned = cmd.clone();
            cloned.createArgument(true).setLine(c);
            this.addConfiguredCommandline(cloned, true);
        }

        try {
            commandlines.forEach(this::runCommand);
        } finally {
            if (cloned != null) {
                removeCommandline(cloned);
            }
            setCommand(savedCommand);
            FileUtils.close(outputStream);
            FileUtils.close(errorStream);
        }
    }

    private String executeToString(Execute execute) {

        String cmdLine = Commandline.describeCommand(execute
                .getCommandline());
        StringBuilder buf = removeCvsPassword(cmdLine);

        String newLine = StringUtils.LINE_SEP;
        String[] variableArray = execute.getEnvironment();

        if (variableArray != null) {
            buf.append(newLine);
            buf.append(newLine);
            buf.append("environment:");
            buf.append(newLine);
            for (int z = 0; z < variableArray.length; z++) {
                buf.append(newLine);
                buf.append("\t");
                buf.append(variableArray[z]);
            }
        }

        return buf.toString();
    }

    /**
     * Removes the cvs password from the command line, if given on the command
     * line. This password can be given on the command line in the cvsRoot
     * -d:pserver:user:password@server:path
     * It has to be noted that the password may be omitted altogether.
     * @param cmdLine the CVS command line
     * @return a StringBuffer where the password has been removed (if available)
     */
    private StringBuilder removeCvsPassword(String cmdLine) {
        StringBuilder buf = new StringBuilder(cmdLine);

        int start = cmdLine.indexOf("-d:");

        if (start >= 0) {
            int stop = cmdLine.indexOf('@', start);
            int startproto = cmdLine.indexOf(':', start);
            int startuser = cmdLine.indexOf(':', startproto + 1);
            int startpass = cmdLine.indexOf(':', startuser + 1);
            stop = cmdLine.indexOf('@', start);
            if (stop >= 0 && startpass > startproto && startpass < stop) {
                for (int i = startpass + 1; i < stop; i++) {
                    buf.replace(i, i + 1, "*");
                }
            }
        }
        return buf;
    }

    /**
     * The CVSROOT variable.
     *
     * @param root
     *            the CVSROOT variable
     */
    public void setCvsRoot(String root) {

        // Check if not real cvsroot => set it to null
        if (root != null && root.trim().isEmpty()) {
            root = null;
        }

        this.cvsRoot = root;
    }

    /**
     * access the CVSROOT variable
     * @return CVSROOT
     */
    public String getCvsRoot() {

        return this.cvsRoot;
    }

    /**
     * The CVS_RSH variable.
     *
     * @param rsh the CVS_RSH variable
     */
    public void setCvsRsh(String rsh) {
        if (rsh != null && rsh.trim().isEmpty()) {
            rsh = null;
        }

        this.cvsRsh = rsh;
    }

    /**
     * access the CVS_RSH variable
     * @return the CVS_RSH variable
     */
    public String getCvsRsh() {

        return this.cvsRsh;
    }

    /**
     * Port used by CVS to communicate with the server.
     *
     * @param port port of CVS
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * access the port of CVS
     * @return the port of CVS
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Password file to read passwords from.
     *
     * @param passFile password file to read passwords from
     */
    public void setPassfile(File passFile) {
        this.passFile = passFile;
    }

    /**
     * find the password file
     * @return password file
     */
    public File getPassFile() {
        return this.passFile;
    }

    /**
     * The directory where the checked out files should be placed.
     *
     * <p>Note that this is different from CVS's -d command line
     * switch as Ant will never shorten pathnames to avoid empty
     * directories.</p>
     *
     * @param dest directory where the checked out files should be placed
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    /**
     * get the file where the checked out files should be placed
     *
     * @return directory where the checked out files should be placed
     */
    public File getDest() {
        return this.dest;
    }

    /**
     * The package/module to operate upon.
     *
     * @param p package or module to operate upon
     */
    public void setPackage(String p) {
        this.cvsPackage = p;
    }

    /**
     * access the package or module to operate upon
     *
     * @return package/module
     */
    public String getPackage() {
        return this.cvsPackage;
    }
    /**
     * tag or branch
     * @return tag or branch
     * @since ant 1.6.1
     */
    public String getTag() {
        return tag;
    }

    /**
     * The tag of the package/module to operate upon.
     * @param p tag
     */
    public void setTag(String p) {
        // Check if not real tag => set it to null
        if (!(p == null || p.trim().isEmpty())) {
            tag = p;
            addCommandArgument("-r" + p);
        }
    }

    /**
     * This needs to be public to allow configuration
     *      of commands externally.
     * @param arg command argument
     */
    public void addCommandArgument(String arg) {
        this.addCommandArgument(cmd, arg);
    }

    /**
     * This method adds a command line argument to an external command.
     *
     * I do not understand what this method does in this class ???
     * particularly not why it is public ????
     * AntoineLL July 23d 2003
     *
     * @param c  command line to which one argument should be added
     * @param arg argument to add
     */
    public void addCommandArgument(Commandline c, String arg) {
        c.createArgument().setValue(arg);
    }


    /**
     * Use the most recent revision no later than the given date.
     * @param p a date as string in a format that the CVS executable
     * can understand see man cvs
     */
    public void setDate(String p) {
        if (!(p == null || p.trim().isEmpty())) {
            addCommandArgument("-D");
            addCommandArgument(p);
        }
    }

    /**
     * The CVS command to execute.
     *
     * This should be deprecated, it is better to use the Commandline class ?
     * AntoineLL July 23d 2003
     *
     * @param c a command as string
     */
    public void setCommand(String c) {
        this.command = c;
    }
    /**
     * accessor to a command line as string
     *
     * This should be deprecated
     * AntoineLL July 23d 2003
     *
     * @return command line as string
     */
    public String getCommand() {
        return this.command;
    }

    /**
     * If true, suppress informational messages.
     * @param q  if true, suppress informational messages
     */
    public void setQuiet(boolean q) {
        quiet = q;
    }

    /**
     * If true, suppress all messages.
     * @param q  if true, suppress all messages
     * @since Ant 1.6
     */
    public void setReallyquiet(boolean q) {
        reallyquiet = q;
    }

    /**
     * If true, report only and don't change any files.
     *
     * @param ne if true, report only and do not change any files.
     */
    public void setNoexec(boolean ne) {
        noexec = ne;
    }

    /**
     * The file to direct standard output from the command.
     * @param output a file to which stdout should go
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * The file to direct standard error from the command.
     *
     * @param error a file to which stderr should go
     */
    public void setError(File error) {
        this.error = error;
    }

    /**
     * Whether to append output/error when redirecting to a file.
     * @param value true indicated you want to append
     */
    public void setAppend(boolean value) {
        this.append = value;
    }

    /**
     * Stop the build process if the command exits with
     * a return code other than 0.
     * Defaults to false.
     * @param failOnError stop the build process if the command exits with
     * a return code other than 0
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Configure a commandline element for things like cvsRoot, quiet, etc.
     * @param c the command line which will be configured
     * if the commandline is initially null, the function is a noop
     * otherwise the function append to the commandline arguments concerning
     * <ul>
     * <li>
     * cvs package
     * </li>
     * <li>
     * compression
     * </li>
     * <li>
     * quiet or reallyquiet
     * </li>
     * <li>cvsroot</li>
     * <li>noexec</li>
     * </ul>
     */
    protected void configureCommandline(Commandline c) {
        if (c == null) {
            return;
        }
        c.setExecutable("cvs");
        if (cvsPackage != null) {
            c.createArgument().setLine(cvsPackage);
        }
        for (Module m : modules) {
            c.createArgument().setValue(m.getName());
        }
        if (this.compression > 0
            && this.compression <= MAXIMUM_COMRESSION_LEVEL) {
            c.createArgument(true).setValue("-z" + this.compression);
        }
        if (quiet && !reallyquiet) {
            c.createArgument(true).setValue("-q");
        }
        if (reallyquiet) {
            c.createArgument(true).setValue("-Q");
        }
        if (noexec) {
            c.createArgument(true).setValue("-n");
        }
        if (cvsRoot != null) {
            c.createArgument(true).setLine("-d" + cvsRoot);
        }
    }

    /**
     * remove a particular command from a vector of command lines
     * @param c command line which should be removed
     */
    protected void removeCommandline(Commandline c) {
        commandlines.remove(c);
    }

    /**
     * Adds direct command-line to execute.
     * @param c command line to execute
     */
    public void addConfiguredCommandline(Commandline c) {
        this.addConfiguredCommandline(c, false);
    }

    /**
     * Configures and adds the given Commandline.
     * @param c commandline to insert
     * @param insertAtStart If true, c is
     * inserted at the beginning of the vector of command lines
    */
    public void addConfiguredCommandline(Commandline c,
                                         boolean insertAtStart) {
        if (c == null) {
            return;
        }
        this.configureCommandline(c);
        if (insertAtStart) {
            commandlines.add(0, c);
        } else {
            commandlines.add(c);
        }
    }

    /**
    * If set to a value 1-9 it adds -zN to the cvs command line, else
    * it disables compression.
     * @param level compression level 1 to 9
    */
    public void setCompressionLevel(int level) {
        this.compression = level;
    }

    /**
     * If true, this is the same as compressionlevel="3".
     *
     * @param usecomp If true, turns on compression using default
     * level, AbstractCvsTask.DEFAULT_COMPRESSION_LEVEL.
     */
    public void setCompression(boolean usecomp) {
        setCompressionLevel(usecomp
            ? AbstractCvsTask.DEFAULT_COMPRESSION_LEVEL : 0);
    }

    /**
     * add a named module/package.
     *
     * @since Ant 1.8.0
     */
    public void addModule(Module m) {
        modules.add(m);
    }

    protected List<Module> getModules() {
        return new ArrayList<>(modules);
    }

    public static final class Module {
        private String name;

        public void setName(String s) {
            name = s;
        }
        public String getName() {
            return name;
        }
    }

}
