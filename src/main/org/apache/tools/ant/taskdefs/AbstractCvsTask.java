/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.util.StringUtils;

/**
 * original Cvs.java 1.20
 *
 *  NOTE: This implementation has been moved here from Cvs.java with
 *  the addition of some accessors for extensibility.  Another task
 *  can extend this with some customized output processing.
 *
 * @author costin@dnt.ro
 * @author stefano@apache.org
 * @author Wolfgang Werner 
 *         <a href="mailto:wwerner@picturesafe.de">wwerner@picturesafe.de</a>
 * @author Kevin Ross 
 *         <a href="mailto:kevin.ross@bredex.com">kevin.ross@bredex.com</a>
 *
 * @since Ant 1.5
 */
public abstract class AbstractCvsTask extends Task {
    /** 
     * Default compression level to use, if compression is enabled via
     * setCompression( true ). 
     */
    public static final int DEFAULT_COMPRESSION_LEVEL = 3;

    private Commandline cmd = new Commandline();

    /** list of Commandline children */
    private Vector vecCommandlines = new Vector();

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
     * the default command.
     */
    private static final String default_command = "checkout";
    /**
     * the CVS command to execute.
     */
    private String command = null;

    /**
     * suppress information messages.
     */
    private boolean quiet = false;

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

    /** empty no-arg constructor*/
    public AbstractCvsTask() {
        super();
    }

    public void setExecuteStreamHandler(ExecuteStreamHandler handler) {
        this.executeStreamHandler = handler;
    }

    protected ExecuteStreamHandler getExecuteStreamHandler() {

        if (this.executeStreamHandler == null) {
            setExecuteStreamHandler(new PumpStreamHandler(getOutputStream(), 
                                                          getErrorStream()));
        }

        return this.executeStreamHandler;
    }


    protected void setOutputStream(OutputStream outputStream) {

        this.outputStream = outputStream;
    }

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
                    throw new BuildException(e, location);
                }
            } else {
                setOutputStream(new LogOutputStream(this, Project.MSG_INFO));
            }
        }

        return this.outputStream;
    }

    protected void setErrorStream(OutputStream errorStream) {

        this.errorStream = errorStream;
    }

    protected OutputStream getErrorStream() {

        if (this.errorStream == null) {

            if (error != null) {

                try {
                    setErrorStream(new PrintStream(
                                       new BufferedOutputStream(
                                           new FileOutputStream(error.getPath(),
                                                                append))));
                } catch (IOException e) {
                    throw new BuildException(e, location);
                }
            } else {
                setErrorStream(new LogOutputStream(this, Project.MSG_WARN));
            }
        }

        return this.errorStream;
    }

    /**
     * Sets up the environment for toExecute and then runs it.
     * @throws BuildException
     */
    protected void runCommand(Commandline toExecute) throws BuildException {
        // XXX: we should use JCVS (www.ice.com/JCVS) instead of
        // command line execution so that we don't rely on having
        // native CVS stuff around (SM)

        // We can't do it ourselves as jCVS is GPLed, a third party task
        // outside of jakarta repositories would be possible though (SB).

        Environment env = new Environment();

        if (port > 0) {
            Environment.Variable var = new Environment.Variable();
            var.setKey("CVS_CLIENT_PORT");
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

            if(defaultPassFile.exists()) {
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
                    Project.MSG_INFO);
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

        exe.setAntRun(project);
        if (dest == null) {
            dest = project.getBaseDir();
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
            /*Throw an exception if cvs exited with error. (Iulian)*/
            if (failOnError && retCode != 0) {
                throw new BuildException("cvs exited with error code "
                                         + retCode 
                                         + StringUtils.LINE_SEP
                                         + "Command line was ["
                                         + actualCommandLine + "]", location);
            }
        } catch (IOException e) {
            if (failOnError) {
                throw new BuildException(e, location);
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
                throw new BuildException(e, location);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_WARN);
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
            }
            if (errorStream != null) {
                try {
                    errorStream.close();
                } catch (IOException e) {}
            }
        }
    }

    public void execute() throws BuildException {

        String savedCommand = getCommand();

        if (this.getCommand() == null && vecCommandlines.size() == 0) {
            // re-implement legacy behaviour:
            this.setCommand(AbstractCvsTask.default_command);
        }

        String c = this.getCommand();
        Commandline cloned = null;
        if (c != null) {
            cloned = (Commandline) cmd.clone();
            cloned.createArgument(true).setLine(c);
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
            setCommand(savedCommand);
        }
    }

    private String executeToString(Execute execute){

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
            for (int z = 0; z < variableArray.length; z++){
                stringBuffer.append(newLine);
                stringBuffer.append("\t");
                stringBuffer.append(variableArray[z]);
            }
        }

        return stringBuffer.toString();
    }

    /**
     * The CVSROOT variable.
     *
     * @param root
     */
    public void setCvsRoot(String root) {

        // Check if not real cvsroot => set it to null
        if (root != null) {
            if (root.trim().equals("")) {
                root = null;
            }
        }

        this.cvsRoot = root;
    }

    public String getCvsRoot(){

        return this.cvsRoot;
    }

    /**
     * The CVS_RSH variable.
     *
     * @param rsh
     */
    public void setCvsRsh(String rsh) {
        // Check if not real cvsrsh => set it to null
        if (rsh != null) {
            if (rsh.trim().equals("")) {
                rsh = null;
            }
        }

        this.cvsRsh = rsh;
    }

    public String getCvsRsh(){

        return this.cvsRsh;
    }

    /**
     * Port used by CVS to communicate with the server.
     *
     * @param port
     */
    public void setPort(int port){
        this.port = port;
    }

    public int getPort(){

        return this.port;
    }

    /**
     * Password file to read passwords from.
     *
     * @param passFile
     */
    public void setPassfile(File passFile){
        this.passFile = passFile;
    }

    public File getPassFile(){

        return this.passFile;
    }

    /**
     * The directory where the checked out files should be placed.
     *
     * @param dest
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    public File getDest(){

        return this.dest;
    }

    /**
     * The package/module to operate upon.
     *
     * @param p
     */
    public void setPackage(String p) {
        this.cvsPackage = p;
    }

    public String getPackage(){

        return this.cvsPackage;
    }

    /**
     * The tag of the package/module to operate upon.
     * @param p
     */
    public void setTag(String p) {
        // Check if not real tag => set it to null
        if (p != null && p.trim().length() > 0) {
            addCommandArgument("-r");
            addCommandArgument(p);
        }
    }

    /**
     * This needs to be public to allow configuration
     *      of commands externally.
     */
    public void addCommandArgument(String arg){
        this.addCommandArgument(cmd, arg);
    }

    public void addCommandArgument(Commandline c, String arg){
        c.createArgument().setValue(arg);
    }


    /**
     * Use the most recent revision no later than the given date.
     * @param p
     */
    public void setDate(String p) {
        if (p != null && p.trim().length() > 0) {
            addCommandArgument("-D");
            addCommandArgument(p);
        }
    }

    /**
     * The CVS command to execute.
     * @param c
     */
    public void setCommand(String c) {
        this.command = c;
    }
    public String getCommand() {
        return this.command;
    }

    /**
     * If true, suppress informational messages.
     * @param q
     */
    public void setQuiet(boolean q) {
        quiet = q;
    }

    /**
     * If true, report only and don't change any files.
     *
     * @param ne
     */
    public void setNoexec(boolean ne) {
        noexec = ne;
    }

    /**
     * The file to direct standard output from the command.
     * @param output
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * The file to direct standard error from the command.
     *
     * @param error
     */
    public void setError(File error) {
        this.error = error;
    }

    /**
     * Whether to append output/error when redirecting to a file.
     * @param value
     */
    public void setAppend(boolean value){
        this.append = value;
    }

    /**
     * Stop the build process if the command exits with
     * a return code other than 0.
     * Defaults to false.
     * @param failOnError
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Configure a commandline element for things like cvsRoot, quiet, etc.
     */
    protected void configureCommandline(Commandline c) {
        if (c == null) {
            return;
        }
        c.setExecutable("cvs");
        if (cvsPackage != null) {
            c.createArgument().setLine(cvsPackage);
        }
        if (this.compression > 0 && this.compression < 10) {
            c.createArgument(true).setValue("-z" + this.compression);
        }
        if (quiet) {
            c.createArgument(true).setValue("-q");
        }
        if (noexec) {
            c.createArgument(true).setValue("-n");
        }
        if (cvsRoot != null) {
            c.createArgument(true).setLine("-d" + cvsRoot);
        }
    }

    protected void removeCommandline(Commandline c) {
        vecCommandlines.removeElement(c);
    }

    /**
     * Adds direct command-line to execute.
     * @param c
     */
    public void addConfiguredCommandline(Commandline c) {
        this.addConfiguredCommandline(c, false);
    }

    /**
    * Configures and adds the given Commandline.
    * @param insertAtStart If true, c is
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

    /**
    * If set to a value 1-9 it adds -zN to the cvs command line, else
    * it disables compression.
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
        setCompressionLevel(usecomp ? 
                            AbstractCvsTask.DEFAULT_COMPRESSION_LEVEL : 0);
    }

}
