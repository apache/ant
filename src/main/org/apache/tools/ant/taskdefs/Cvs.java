/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Commandline;
import java.io.*;

/**
 *
 *
 * @author costin@dnt.ro
 * @author stefano@apache.org
 * @author Wolfgang Werner <a href="mailto:wwerner@picturesafe.de">wwerner@picturesafe.de</a>
 */

public class Cvs extends Task {

    private Commandline cmd = new Commandline();
    
    /**
     * the CVSROOT variable.
     */
    private String cvsRoot;

    /**
     * the package/module to check out.
     */
    private String pack;

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
     * the directory where the checked out files should be placed.
     */
    private File dest;

    /**
     * the file to direct standard output from the command.
     */
    private File output;

    /**
     * the file to direct standard error from the command.
     */
    private File error; 

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
        toExecute.addArguments(cmd.getCommandline());

        if (pack != null) {
            toExecute.createArgument().setLine(pack);
        }

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
                    throw new BuildException(e, location);
                }
            }
            else {
                outputstream = new LogOutputStream(this, Project.MSG_INFO);
            }
            if (error != null) {
                try {
                    errorstream = new PrintStream(new BufferedOutputStream(new FileOutputStream(error)));
                } catch (IOException e) {
                    throw new BuildException(e, location);
                }
            }
            else {
                errorstream = new LogOutputStream(this, Project.MSG_WARN);
            }
            streamhandler = new PumpStreamHandler(outputstream, errorstream);
        }

        Execute exe = new Execute(streamhandler, 
                                  null);

        exe.setAntRun(project);
        if (dest == null) dest = project.getBaseDir();
        exe.setWorkingDirectory(dest);

        exe.setCommandline(toExecute.getCommandline());
        try {
            exe.execute();
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

    public void setCvsRoot(String root) {
        // Check if not real cvsroot => set it to null 
        if (root != null) { 
            if (root.trim().equals("")) 
                root = null; 
        } 

        this.cvsRoot = root;
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

    public void setPackage(String p) {
        this.pack = p;
    }

    public void setTag(String p) { 
        // Check if not real tag => set it to null 
        if (p != null && p.trim().length() > 0) {
            cmd.createArgument().setValue("-r");
            cmd.createArgument().setValue(p);
        }
    } 

    
    public void setDate(String p) {
        if(p != null && p.trim().length() > 0) {
            cmd.createArgument().setValue("-D");
            cmd.createArgument().setValue(p);
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
}


