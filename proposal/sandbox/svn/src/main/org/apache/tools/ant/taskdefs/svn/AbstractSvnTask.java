/*
 * Copyright  2005 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.svn;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.util.StringUtils;

/**
 * Just like Ant's built-in CVS task, the main loginc lives in this
 * abstract task.
 */
public abstract class AbstractSvnTask extends Task {

    private Commandline cmd = new Commandline();

    /** list of Commandline children */
    private Vector vecCommandlines = new Vector();

    /**
     * the URL the subcommand should apply to.
     */
    private String svnURL;

    /**
     * the revision
     */
    private String revision;
    /**
     * the default command.
     */
    private static final String DEFAULT_SUBCOMMAND = "checkout";
    /**
     * the SVN command to execute.
     */
    private String subCommand = null;

    /**
     * suppress information messages.
     */
    private boolean quiet = false;

    /**
     * report only, don't change any files.
     */
    private boolean dryrun = false;

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
     * If true it will stop the build if svn exits with error.
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

    /** empty no-arg constructor*/
    public AbstractSvnTask() {
        super();
    }

    /**
     * sets the handler
     * @param handler a handler able of processing the output and error streams from the svn exe
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
     * sets a stream to which the output from the svn executable should be sent
     * @param outputStream stream to which the stdout from svn should go
     */
    protected void setOutputStream(OutputStream outputStream) {

        this.outputStream = outputStream;
    }

    /**
     * access the stream to which the stdout from svn should go
     * if this stream has already been set, it will be returned
     * if the stream has not yet been set, if the attribute output
     * has been set, the output stream will go to the output file
     * otherwise the output will go to ant's logging system
     * @return output stream to which svn' stdout should go to
     */
    protected OutputStream getOutputStream() {

        if (this.outputStream == null) {

            if (output != null) {
                try {
                    setOutputStream(new PrintStream(
                                        new BufferedOutputStream(
                                            new FileOutputStream(output
                                                                 .getPath(),
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
     * sets a stream to which the stderr from the svn exe should go
     * @param errorStream an output stream willing to process stderr
     */
    protected void setErrorStream(OutputStream errorStream) {

        this.errorStream = errorStream;
    }

    /**
     * access the stream to which the stderr from svn should go
     * if this stream has already been set, it will be returned
     * if the stream has not yet been set, if the attribute error
     * has been set, the output stream will go to the file denoted by the error attribute
     * otherwise the stderr output will go to ant's logging system
     * @return output stream to which svn' stderr should go to
     */
    protected OutputStream getErrorStream() {

        if (this.errorStream == null) {

            if (error != null) {

                try {
                    setErrorStream(new PrintStream(
                                       new BufferedOutputStream(
                                           new FileOutputStream(error.getPath(),
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
     * @throws BuildException if failonError is set to true and the svn command fails
     */
    protected void runCommand(Commandline toExecute) throws BuildException {
        Environment env = new Environment();

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
            /*Throw an exception if svn exited with error. (Iulian)*/
            if (failOnError && Execute.isFailure(retCode)) {
                throw new BuildException("svn exited with error code "
                                         + retCode
                                         + StringUtils.LINE_SEP
                                         + "Command line was ["
                                         + actualCommandLine + "]", getLocation());
            }
        } catch (IOException e) {
            if (failOnError) {
                throw new BuildException(e, getLocation());
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_WARN);
            }
        } catch (BuildException e) {
            if (failOnError) {
                throw(e);
            } else {
                Throwable t = e.getException();
                if (t == null) {
                    t = e;
                }
                log("Caught exception: " + t.getMessage(), Project.MSG_WARN);
            }
        } catch (Exception e) {
            if (failOnError) {
                throw new BuildException(e, getLocation());
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_WARN);
            }
        }
    }

    /**
     * do the work
     * @throws BuildException if failonerror is set to true and the svn command fails.
     */
    public void execute() throws BuildException {

        String savedCommand = getSubCommand();

        if (this.getSubCommand() == null && vecCommandlines.size() == 0) {
            // re-implement legacy behaviour:
            this.setSubCommand(AbstractSvnTask.DEFAULT_SUBCOMMAND);
        }

        String c = this.getSubCommand();
        Commandline cloned = null;
        if (c != null) {
            cloned = (Commandline) cmd.clone();
            cloned.createArgument(true).setLine(c);
            if (svnURL != null) {
                cloned.createArgument().setValue(svnURL);
            }
            this.addConfiguredCommandline(cloned, true);
        }

        try {
            for (int i = 0; i < vecCommandlines.size(); i++) {
                this.runCommand((Commandline) vecCommandlines.elementAt(i));
            }
        } finally {
            if (cloned != null) {
                removeCommandline(cloned);
            }
            setSubCommand(savedCommand);

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
            if (errorStream != null) {
                try {
                    errorStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    private String executeToString(Execute execute) {

        StringBuffer stringBuffer =
            new StringBuffer(Commandline.describeCommand(execute
                                                         .getCommandline()));

        String newLine = StringUtils.LINE_SEP;
        String[] variableArray = execute.getEnvironment();

        if (variableArray != null) {
            stringBuffer.append(newLine);
            stringBuffer.append(newLine);
            stringBuffer.append("environment:");
            stringBuffer.append(newLine);
            for (int z = 0; z < variableArray.length; z++) {
                stringBuffer.append(newLine);
                stringBuffer.append("\t");
                stringBuffer.append(variableArray[z]);
            }
        }

        return stringBuffer.toString();
    }

    /**
     * The URL the subcommand should apply to.
     *
     * @param url the URL the subcommand should apply to.
     */
    public void setSvnURL(String url) {

        // Check if not real svnroot => set it to null
        if (url != null) {
            if (url.trim().equals("")) {
                url = null;
            }
        }

        this.svnURL = url;
    }

    /**
     * access the svnURL variable
     * @return svnURL
     */
    public String getSvnURL() {

        return this.svnURL;
    }

    /**
     * The directory where the checked out files should be placed.
     *
     * <p>Note that this is different from SVN's -d command line
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
     * revision
     * @return revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * The revision to operate upon.
     * @param p revision
     */
    public void setRevision(String p) {
        // Check if not real revision => set it to null
        if (p != null && p.trim().length() > 0) {
            revision = p;
            addSubCommandArgument("--revision");
            addSubCommandArgument(p);
        }
    }

    /**
     * This needs to be public to allow configuration
     *      of commands externally.
     * @param arg command argument
     */
    public void addSubCommandArgument(String arg) {
        this.addSubCommandArgument(cmd, arg);
    }

    /**
     * This method adds a command line argument to an external subcommand.
     *
     * @param c  command line to which one argument should be added
     * @param arg argument to add
     */
    protected void addSubCommandArgument(Commandline c, String arg) {
        c.createArgument().setValue(arg);
    }


    /**
     * The SVN command to execute.
     *
     * @param c a command as string
     */
    public void setSubCommand(String c) {
        this.subCommand = c;
    }
    /**
     * accessor to a command line as string
     *
     * @return command line as string
     */
    public String getSubCommand() {
        return this.subCommand;
    }

    /**
     * If true, suppress informational messages.
     * @param q  if true, suppress informational messages
     */
    public void setQuiet(boolean q) {
        quiet = q;
    }

    /**
     * If true, report only and don't change any files.
     *
     * @param ne if true, report only and do not change any files.
     */
    public void setDryrun(boolean ne) {
        dryrun = ne;
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
     * Configure a commandline element for things like quiet, etc.
     * @param c the command line which will be configured
     * if the commandline is initially null, the function is a noop
     * otherwise the function append to the commandline arguments concerning
     * <ul>
     * <li>
     * svn
     * </li>
     * <li>
     * quiet
     * </li>
     * <li>svnroot</li>
     * <li>dryrun</li>
     * </ul>
     */
    protected void configureCommandline(Commandline c) {
        if (c == null) {
            return;
        }
        c.setExecutable("svn");
        if (quiet) {
            c.createArgument(true).setValue("--quiet");
        }
        if (dryrun) {
            c.createArgument(true).setValue("--dry-run");
        }
    }

    /**
     * remove a particular command from a vector of command lines
     * @param c command line which should be removed
     */
    protected void removeCommandline(Commandline c) {
        vecCommandlines.removeElement(c);
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
            vecCommandlines.insertElementAt(c, 0);
        } else {
            vecCommandlines.addElement(c);
        }
    }

}
