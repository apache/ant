/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;

/**
 * original Cvs.java 1.20
 *
 *  NOTE: This implementation has been moved here from Cvs.java with the addition of
 *          some accessors for extensibility.  Another task can extend this with
 *          some customized output processing.
 *
 * @author costin@dnt.ro
 * @author stefano@apache.org
 * @author Wolfgang Werner <a href="mailto:wwerner@picturesafe.de">wwerner@picturesafe.de</a>
 * @author Kevin Ross <a href="mailto:kevin.ross@bredex.com">kevin.ross@bredex.com</a>
 */
public abstract class AbstractCvsTask extends Task {

    private Commandline cmd = new Commandline();

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
     * the CVS command to execute.
     */
    private String command = "checkout";

    /**
     * suppress information messages.
     */
    private boolean quiet = false;

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
     *  the output.
     */
    private ExecuteStreamHandler executeStreamHandler;
    private OutputStream outputStream;
    private OutputStream errorStream;

    public void setExecuteStreamHandler(ExecuteStreamHandler executeStreamHandler){

        this.executeStreamHandler = executeStreamHandler;
    }

    protected ExecuteStreamHandler getExecuteStreamHandler(){

        if(this.executeStreamHandler == null){

            setExecuteStreamHandler(new PumpStreamHandler(getOutputStream(), getErrorStream()));
        }

        return this.executeStreamHandler;
    }


    protected void setOutputStream(OutputStream outputStream){

        this.outputStream = outputStream;
    }

    protected OutputStream getOutputStream(){

        if(this.outputStream == null){

            if (output != null) {
                try {
                    setOutputStream(new PrintStream(new BufferedOutputStream(new FileOutputStream(output.getPath(), append))));
                }
                catch (IOException e) {
                    throw new BuildException(e, location);
                }
            }
            else {
                setOutputStream(new LogOutputStream(this, Project.MSG_INFO));
            }
        }

        return this.outputStream;
    }

    protected void setErrorStream(OutputStream errorStream){

        this.errorStream = errorStream;
    }

    protected OutputStream getErrorStream(){

        if(this.errorStream == null){

            if (error != null) {

                try {
                    setErrorStream(new PrintStream(new BufferedOutputStream(new FileOutputStream(error.getPath(), append))));
                }
                catch (IOException e) {
                    throw new BuildException(e, location);
                }
            }
            else {
                setErrorStream(new LogOutputStream(this, Project.MSG_WARN));
            }
        }

        return this.errorStream;
    }

    public void execute() throws BuildException {

        // XXX: we should use JCVS (www.ice.com/JCVS) instead of command line
        // execution so that we don't rely on having native CVS stuff around (SM)

        // We can't do it ourselves as jCVS is GPLed, a third party task
        // outside of jakarta repositories would be possible though (SB).

        Commandline toExecute = new Commandline();

        toExecute.setExecutable("cvs");
        if (cvsRoot != null) {
            toExecute.createArgument().setValue("-d");
            toExecute.createArgument().setValue(cvsRoot);
        }
        if (noexec) {
            toExecute.createArgument().setValue("-n");
        }
        if (quiet) {
            toExecute.createArgument().setValue("-q");
        }

        toExecute.createArgument().setLine(command);

        //
        // get the other arguments.
        //
        toExecute.addArguments(cmd.getCommandline());

        if (cvsPackage != null) {
            toExecute.createArgument().setLine(cvsPackage);
        }

        Environment env = new Environment();

        if(port>0){
            Environment.Variable var = new Environment.Variable();
            var.setKey("CVS_CLIENT_PORT");
            var.setValue(String.valueOf(port));
            env.addVariable(var);
        }

        /**
         * Need a better cross platform integration with <cvspass>, so use the same filename.
         */
        /* But currently we cannot because 'cvs log' is not working with a pass file.
        if(passFile == null){

            File defaultPassFile = new File(System.getProperty("user.home") + File.separatorChar + ".cvspass");

            if(defaultPassFile.exists())
                this.setPassfile(defaultPassFile);
        }
         */

        if(passFile!=null){
            Environment.Variable var = new Environment.Variable();
            var.setKey("CVS_PASSFILE");
            var.setValue(String.valueOf(passFile));
            env.addVariable(var);
            log("Using cvs passfile: " + String.valueOf(passFile), Project.MSG_INFO);
        }

        if(cvsRsh!=null){
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

        exe.setWorkingDirectory(dest);
        exe.setCommandline(toExecute.getCommandline());
        exe.setEnvironment(env.getVariables());

        try {
            log("Executing: " + executeToString(exe), Project.MSG_DEBUG);

            int retCode = exe.execute();
            /*Throw an exception if cvs exited with error. (Iulian)*/
            if(failOnError && retCode != 0) {
                throw new BuildException("cvs exited with error code "+ retCode);
            }
        }
        catch (IOException e) {
            throw new BuildException(e, location);
        }
        finally {
            //
            // condition used to be if(output == null) outputStream.close().  This is
            //      not appropriate.  Check if the stream itself is not null, then close().
            //
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

    private String executeToString(Execute execute){

        StringBuffer stringBuffer = new StringBuffer(250);
        String[] commandLine = execute.getCommandline();
        for(int i=0; i<commandLine.length; i++){

            stringBuffer.append(commandLine[i]);
            stringBuffer.append(" ");
        }
        String newLine = System.getProperty("line.separator");
        stringBuffer.append(newLine);
        stringBuffer.append(newLine);
        stringBuffer.append("environment:");
        stringBuffer.append(newLine);


        String[] variableArray = execute.getEnvironment();

        if(variableArray != null){
            for(int z=0; z<variableArray.length; z++){

                stringBuffer.append(newLine);
                stringBuffer.append("\t");
                stringBuffer.append(variableArray[z]);
            }
        }

        return stringBuffer.toString();
    }

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

    public void setPort(int port){
        this.port = port;
    }

    public int getPort(){

        return this.port;
    }

    public void setPassfile(File passFile){
        this.passFile = passFile;
    }

    public File getPassFile(){

        return this.passFile;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

    public File getDest(){

        return this.dest;
    }

    public void setPackage(String p) {
        this.cvsPackage = p;
    }

    public String getPackage(){

        return this.cvsPackage;
    }

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

        this.cmd.createArgument().setValue(arg);
    }

    public void setDate(String p) {
        if(p != null && p.trim().length() > 0) {
            addCommandArgument("-D");
            addCommandArgument(p);
        }
    }

    public void setCommand(String c) {
        this.command = c;
    }

    public void setQuiet(boolean q) {
        quiet = q;
    }

    public void setNoexec(boolean ne) {
        noexec = ne;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setError(File error) {
        this.error = error;
    }

    public void setAppend(boolean value){
        this.append = value;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }
}


