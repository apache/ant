/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights 
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Vector;

/**
 * Launcher for Java applications. Allows use of
 * the same JVM for the called application thus resulting in much
 * faster operation.
 *
 * @author Stefano Mazzocchi 
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
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
    private File out;
    private PrintStream outStream = null;
    private boolean failOnError = false;
    private boolean append = false;
    private Long timeout = null;
    
    /**
     * Do the execution.
     */
    public void execute() throws BuildException {
        File savedDir = dir;

        int err = -1;
        try {
            if ((err = executeJava()) != 0) { 
                if (failOnError) {
                    throw new BuildException("Java returned: " + err, location);
                } else {
                    log("Java Result: " + err, Project.MSG_ERR);
                }
            }
        } finally {
            dir = savedDir;
        }
    }

    /**
     * Do the execution and return a return code.
     *
     * @return the return code from the execute java class if it was
     * executed in a separate VM (fork = "yes").
     */
    public int executeJava() throws BuildException {
        String classname = cmdl.getClassname();
        if (classname == null && cmdl.getJar() == null) {
            throw new BuildException("Classname must not be null.");
        }

        if (!fork && cmdl.getJar() != null){
            throw new BuildException("Cannot execute a jar in non-forked mode."
                                     + " Please set fork='true'. ");
        }

        if (fork) {
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

            log("Running in same VM " + cmdl.describeJavaCommand(), 
                Project.MSG_VERBOSE);
        }
        
        try {
            if (fork) {
                return run(cmdl.getCommandline());
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
     * Set the classpath to be used when running the Java class
     * 
     * @param s an Ant Path object containing the classpath.
     */
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }
    
    /**
     * Adds a path to the classpath.
     */
    public Path createClasspath() {
        return cmdl.createClasspath(project).createPath();
    }

    /**
     * Classpath to use, by reference.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * The location of the JAR file to execute.
     */
    public void setJar(File jarfile) throws BuildException {
        if (cmdl.getClassname() != null){
            throw new BuildException("Cannot use 'jar' and 'classname' "
                                     + "attributes in same command.");
        }
        cmdl.setJar(jarfile.getAbsolutePath());
    }

    /**
     * Sets the Java class to execute.
     */
    public void setClassname(String s) throws BuildException {
        if (cmdl.getJar() != null){
            throw new BuildException("Cannot use 'jar' and 'classname' "
                                     + "attributes in same command");
        }
        cmdl.setClassname(s);
    }

    /**
     * Deprecated: use nested arg instead.
     * Set the command line arguments for the class.
     * @ant.attribute ignore="true"
     */
    public void setArgs(String s) {
        log("The args attribute is deprecated. " +
            "Please use nested arg elements.",
            Project.MSG_WARN);
        cmdl.createArgument().setLine(s);
    }

    /**
     * Adds a command-line argument.
     */
    public Commandline.Argument createArg() {
        return cmdl.createArgument();
    }

    /**
     * If true, execute in a new VM.
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
     * Adds a JVM argument.
     */
    public Commandline.Argument createJvmarg() {
        return cmdl.createVmArgument();
    }

    /**
     * Set the command used to start the VM (only if not forking).
     */
    public void setJvm(String s) {
        cmdl.setVm(s);
    }
        
    /**
     * Adds a system property.
     */
    public void addSysproperty(Environment.Variable sysp) {
        cmdl.addSysproperty(sysp);
    }

    /**
     * If true, then fail if the command exits with a
     * returncode other than 0
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
     * Corresponds to -mx or -Xmx depending on VM version.
     */
    public void setMaxmemory(String max){
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
     * @since Ant 1.5
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * If true, append output to existing file.
     *
     * @since Ant 1.5
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     *
     * @since Ant 1.5
     */
    public void setTimeout(Long value) {
        timeout = value;
    }

    /**
     * Pass output sent to System.out to specified output file.
     *
     * @since Ant 1.5
     */
    protected void handleOutput(String line) {
        if (outStream != null) {
            outStream.println(line);
        } else {
            super.handleOutput(line);
        }
    }
    
    /**
     * Pass output sent to System.out to specified output file.
     *
     * @since Ant 1.5.2
     */
    protected void handleFlush(String line) {
        if (outStream != null) {
            outStream.print(line);
        } else {
            super.handleFlush(line);
        }
    }
    
    /**
     * Pass output sent to System.err to specified output file.
     *
     * @since Ant 1.5
     */
    protected void handleErrorOutput(String line) {
        if (outStream != null) {
            outStream.println(line);
        } else {
            super.handleErrorOutput(line);
        }
    }
    
    /**
     * Pass output sent to System.err to specified output file.
     *
     * @since Ant 1.5.2
     */
    protected void handleErrorFlush(String line) {
        if (outStream != null) {
            outStream.println(line);
        } else {
            super.handleErrorOutput(line);
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
        exe.setTimeout(timeout);
        if (out != null) {
            try {
                outStream = 
                    new PrintStream(new FileOutputStream(out.getAbsolutePath(),
                                                         append));
                exe.execute(getProject());
                System.out.flush();
                System.err.flush();
            } catch (IOException io) {
                throw new BuildException(io, location);
            } finally {
                if (outStream != null) {
                    outStream.close();
                }
            }
        } else {
            exe.execute(project);
        }
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
                                  createWatchdog());
            } else {
                fos = new FileOutputStream(out.getAbsolutePath(), append);
                exe = new Execute(new PumpStreamHandler(fos),
                                  createWatchdog());
            }
            
            exe.setAntRun(project);
            
            if (dir == null) {
                dir = project.getBaseDir();
            } else if (!dir.exists() || !dir.isDirectory()) {
                throw new BuildException(dir.getAbsolutePath()
                                         + " is not a valid directory",
                                         location);
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
                if (exe.killedProcess()) {
                    log("Timeout: killed the sub-process", Project.MSG_WARN); 
                }
                return rc;
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
     * @since Ant 1.5
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog(timeout.longValue());
    }

}
