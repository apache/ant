/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Commandline;

/**
 *
 *  @author lucas@collab.net
 */
public class Rpm extends Task {
    
    /**
     * the spec file
     */
    private String specFile;

    /**
     * the rpm top dir
     */
    private File topDir;

    /**
     * the rpm command to use
     */
    private String command = "-bb";

    /**
     * clean BUILD directory
     */
    private boolean cleanBuildDir = false;

    /**
     * remove spec file
     */
    private boolean removeSpec = false;

    /**
     * remove sources
     */
    private boolean removeSource = false;

    /**
     * the file to direct standard output from the command
     */
    private File output;

    /**
     * the file to direct standard error from the command
     */
    private File error;

    public void execute() throws BuildException {
        
        Commandline toExecute = new Commandline();

        toExecute.setExecutable("rpm");
        if (topDir != null) {
            toExecute.createArgument().setValue("--define");
            toExecute.createArgument().setValue("_topdir" + topDir);
        }

        toExecute.createArgument().setLine(command);

        if (cleanBuildDir) {
            toExecute.createArgument().setValue("--clean");
        }
        if (removeSpec) {
            toExecute.createArgument().setValue("--rmspec");
        }
        if (removeSource) {
            toExecute.createArgument().setValue("--rmsource");
        }

        toExecute.createArgument().setValue("SPECS/" + specFile);

        ExecuteStreamHandler streamhandler = null;
        OutputStream outputstream = null;
        OutputStream errorstream = null;
        if (error == null && output == null) {
            streamhandler = new LogStreamHandler(this, Project.MSG_INFO,
                                                 Project.MSG_WARN);
        }
        else {
            if (output != null) {
                try {
                    outputstream = new PrintStream(new BufferedOutputStream(new FileOutputStream(output)));
                } catch (IOException e) {
                    throw new BuildException(e,location);
                }
            }
            else {
                outputstream = new LogOutputStream(this,Project.MSG_INFO);
            }
            if (error != null) {
                try {
                    errorstream = new PrintStream(new BufferedOutputStream(new FileOutputStream(error)));
                }  catch (IOException e) {
                    throw new BuildException(e,location);
                }
            }
            else {
                errorstream = new LogOutputStream(this, Project.MSG_WARN);
            }
            streamhandler = new PumpStreamHandler(outputstream, errorstream);
        }

        Execute exe = new Execute(streamhandler, null);

        exe.setAntRun(project);
        if (topDir == null) topDir = project.getBaseDir();
        exe.setWorkingDirectory(topDir);

        exe.setCommandline(toExecute.getCommandline());
        try {
            exe.execute();
            log("Building the RPM based on the " + specFile + " file");
        } catch (IOException e) {
            throw new BuildException(e, location);
        } finally {
            if (output != null) {
                try {
                    outputstream.close();
                } catch (IOException e) {}
            }
            if (error != null) {
                try {
                    errorstream.close();
                } catch (IOException e) {}
            }
        }
    }

    public void setTopDir(File td) {
        this.topDir = td;
    }

    public void setCommand(String c) {
        this.command = c;
    }

    public void setSpecFile(String sf) {
        if ( (sf == null) || (sf.trim().equals(""))) {
            throw new BuildException("You must specify a spec file",location);
        }
        this.specFile = sf;
    }

    public void setCleanBuildDir(boolean cbd) {
        cleanBuildDir = cbd;
    }

    public void setRemoveSpec(boolean rs) {
        removeSpec = rs;
    }

    public void setRemoveSource(boolean rs) {
        removeSource = rs;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public void setError(File error) {
        this.error = error;
    }
}
