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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This task acts as a loader for java applications but allows to use the same JVM 
 * for the called application thus resulting in much faster operation.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Java extends Task {

    private CommandlineJava cmdl = new CommandlineJava();
    private boolean fork = false;
    private File dir = null;
    private File out;
    private boolean failOnError = false;
    
    /**
     * Do the execution.
     */
    public void execute() throws BuildException {
        int err = -1;
        if ((err = executeJava()) != 0) {
            if (failOnError) {
                throw new BuildException("Java returned: "+err, location);
            } else {
                log("Java Result: " + err, Project.MSG_ERR);
            }
        }
    }

    /**
     * Do the execution and return a return code.
     *
     * @return the return code from the execute java class if it was executed in 
     * a separate VM (fork = "yes").
     */
    public int executeJava() throws BuildException {
        String classname = cmdl.getClassname();

        if (classname == null) {
            throw new BuildException("Classname must not be null.");
        }

        if (fork) {
            log("Forking " + cmdl.toString(), Project.MSG_VERBOSE);
        
            return run(cmdl.getCommandline());
        } else {
            if (cmdl.getVmCommand().size() > 1) {
                log("JVM args ignored when same JVM is used.", Project.MSG_WARN);
            }
            if (dir != null) {
                log("Working directory ignored when same JVM is used.", Project.MSG_WARN);
            }

            log("Running in same VM " + cmdl.getJavaCommand().toString(), 
                Project.MSG_VERBOSE);
            run(cmdl);
            return 0;
        }
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }
    
    /**
     * Creates a nested classpath element
     */
    public Path createClasspath() {
        return cmdl.createClasspath(project).createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the class name.
     */
    public void setClassname(String s) {
        cmdl.setClassname(s);
    }

    /**
     * Set the command line arguments for the class.
     */
    public void setArgs(String s) {
        log("The args attribute is deprecated. " +
            "Please use nested arg elements.",
            Project.MSG_WARN);
        cmdl.createArgument().setLine(s);
    }

    /**
     * Creates a nested arg element.
     */
    public Commandline.Argument createArg() {
        return cmdl.createArgument();
    }

    /**
     * Set the forking flag.
     */
    public void setFork(boolean s) {
        this.fork = s;
    }

    /**
     * Set the command line arguments for the JVM.
     */
    public void setJvmargs(String s) {
        log("The jvmargs attribute is deprecated. " +
            "Please use nested jvmarg elements.",
            Project.MSG_WARN);
        cmdl.createVmArgument().setLine(s);
    }
        
    /**
     * Creates a nested jvmarg element.
     */
    public Commandline.Argument createJvmarg() {
        return cmdl.createVmArgument();
    }

    /**
     * Set the command used to start the VM (only if fork==false).
     */
    public void setJvm(String s) {
        cmdl.setVm(s);
    }
        
    /**
     * Add a nested sysproperty element.
     */
    public void addSysproperty(Environment.Variable sysp) {
        cmdl.addSysproperty(sysp);
    }

    /**
     * Throw a BuildException if process returns non 0.
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * The working directory of the process
     */
    public void setDir(File d) {
        this.dir = d;
    }

    /**
     * File the output of the process is redirected to.
     */
    public void setOutput(File out) {
        this.out = out;
    }

    /**
     * -mx or -Xmx depending on VM version
     */
    public void setMaxmemory(String max){
        if (Project.getJavaVersion().startsWith("1.1")) {
            createJvmarg().setValue("-mx"+max);
        } else {
            createJvmarg().setValue("-Xmx"+max);
        }
    }

    /**
     * Executes the given classname with the given arguments as it
     * was a command line application.
     */
    private void run(CommandlineJava command) throws BuildException {
        ExecuteJava exe = new ExecuteJava();
        exe.setJavaCommand(command.getJavaCommand());
        exe.setClasspath(command.getClasspath());
        exe.setSystemProperties(command.getSystemProperties());
        if (out != null) {
            try {
                exe.setOutput(new PrintStream(new FileOutputStream(out)));
            } catch (IOException io) {
                throw new BuildException(io, location);
            }
        }
        
        exe.execute(project);
    }

    /**
     * Executes the given classname with the given arguments in a separate VM.
     */
    private int run(String[] command) throws BuildException {
        FileOutputStream fos = null;
        try {
            Execute exe = null;
            if (out == null) {
                exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                       Project.MSG_WARN), 
                                  null);
            } else {
                fos = new FileOutputStream(out);
                exe = new Execute(new PumpStreamHandler(fos), null);
            }
            
            exe.setAntRun(project);
            
            if (dir == null) {
                dir = project.getBaseDir();
            } else if (!dir.exists() || !dir.isDirectory()) {
                throw new BuildException(dir.getAbsolutePath()+" is not a valid directory",
                                         location);
            }
            
            exe.setWorkingDirectory(dir);
            
            exe.setCommandline(command);
            try {
                return exe.execute();
            } catch (IOException e) {
                throw new BuildException(e, location);
            }
        } catch (IOException io) {
            throw new BuildException(io, location);
        } finally {
            if (fos != null) {
                try {fos.close();} catch (IOException io) {}
            }
        }
    }

    /**
     * Executes the given classname with the given arguments as it
     * was a command line application.
     */
    protected void run(String classname, Vector args) throws BuildException {
        CommandlineJava cmdj = new CommandlineJava();
        cmdj.setClassname(classname);
        for (int i=0; i<args.size(); i++) {
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
}
