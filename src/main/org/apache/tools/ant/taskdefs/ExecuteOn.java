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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
import org.apache.tools.ant.types.*;

import java.util.Vector;
import java.io.File;
import java.io.IOException;

/**
 * Executes a given command, supplying a set of files as arguments. 
 *
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a> 
 */
public class ExecuteOn extends Task {

    private Vector filesets = new Vector();
    private Commandline command = new Commandline();
    private Environment env = new Environment();
    private Integer timeout = null;
    private boolean failOnError = false;

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * The executable.
     */
    public void setExecutable(String exe) {
        command.setExecutable(exe);
    }

    /**
     * Adds an argument to the command (nested arg element)
     */
    public Commandline.Argument createArg() {
        return command.createArgument();
    }

    /**
     * Adds an environment variable (nested env element)
     */
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }

    /**
     * Milliseconds we allow the process to run before we kill it.
     */
    public void setTimeout(Integer value) {
        timeout = value;
    }

    /**
     * throw a build exception if process returns non 0?
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    public void execute() throws BuildException {
        if (command.getExecutable() == null) {
            throw new BuildException("no executable specified", location);
        }

        if (filesets.size() == 0) {
            throw new BuildException("no filesets specified", location);
        }

        String[] orig = command.getCommandline();
        String[] cmd = new String[orig.length+1];
        System.arraycopy(orig, 0, cmd, 0, orig.length);

        Vector v = new Vector();
        for (int i=0; i<filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            String[] s = ds.getIncludedFiles();
            for (int j=0; j<s.length; j++) {
                v.addElement(new File(fs.getDir(), s[j]).getAbsolutePath());
            }
        }
        
        String label = command.toString()+" ";
        String[] environment = env.getVariables();
        for (int i=0; i<v.size(); i++) {
            try {
                // show the command
                String file = (String) v.elementAt(i);
                log(label+file, Project.MSG_VERBOSE);

                Execute exe = new Execute(createHandler(), createWatchdog());
                cmd[orig.length] = file;
                exe.setCommandline(cmd);
                exe.setEnvironment(environment);
                int err = exe.execute();
                if (err != 0) {
                    if (failOnError) {
                        throw new BuildException("Exec returned: "+err, location);
                    } else {
                        log("Result: " + err, Project.MSG_ERR);
                    }
                }
            } catch (IOException e) {
                throw new BuildException("Execute failed: " + e, e, location);
            }
        }
    }

    protected ExecuteStreamHandler createHandler() throws BuildException {
            return new LogStreamHandler(this,
                                        Project.MSG_INFO, Project.MSG_WARN);
    }

    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) return null;
        return new ExecuteWatchdog(timeout.intValue());
    }

}
