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
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.TimeoutObserver;
import org.apache.tools.ant.util.Watchdog;

/**
 * Execute a Java class.
 * @since Ant 1.2
 */
public class ExecuteJava implements Runnable, TimeoutObserver {

    private Commandline javaCommand = null;
    private Path classpath = null;
    private CommandlineJava.SysProperties sysProperties = null;
    private Permissions  perm = null;
    private Method main = null;
    private Long timeout = null;
    private volatile Throwable caught = null;
    private volatile boolean timedOut = false;
    private boolean done = false;
    private Thread thread = null;

    /**
     * Set the Java "command" for this ExecuteJava.
     * @param javaCommand the classname and arguments in a Commandline.
     */
    public void setJavaCommand(Commandline javaCommand) {
        this.javaCommand = javaCommand;
    }

    /**
     * Set the classpath to be used when running the Java class.
     *
     * @param p an Ant Path object containing the classpath.
     */
    public void setClasspath(Path p) {
        classpath = p;
    }

    /**
     * Set the system properties to use when running the Java class.
     * @param s CommandlineJava system properties.
     */
    public void setSystemProperties(CommandlineJava.SysProperties s) {
        sysProperties = s;
    }

    /**
     * Set the permissions for the application run.
     * @param permissions the Permissions to use.
     * @since Ant 1.6
     */
    public void setPermissions(Permissions permissions) {
        perm = permissions;
    }

    /**
     * Set the stream to which all output (System.out as well as System.err)
     * will be written.
     * @param out the PrintStream where output should be sent.
     * @deprecated since 1.4.x.
     *             manage output at the task level.
     */
    @Deprecated
    public void setOutput(PrintStream out) {
    }

    /**
     * Set the timeout for this ExecuteJava.
     * @param timeout timeout as Long.
     * @since Ant 1.5
     */
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    /**
     * Execute the Java class against the specified Ant Project.
     * @param project the Project to use.
     * @throws BuildException on error.
     */
    public void execute(Project project) throws BuildException {
        final String classname = javaCommand.getExecutable();

        AntClassLoader loader = null;
        try {
            if (sysProperties != null) {
                sysProperties.setSystem();
            }
            Class<?> target;
            try {
                if (classpath == null) {
                    target = Class.forName(classname);
                } else {
                    loader = project.createClassLoader(classpath);
                    loader.setParent(project.getCoreLoader());
                    loader.setParentFirst(false);
                    loader.addJavaLibraries();
                    loader.setIsolated(true);
                    loader.setThreadContextLoader();
                    loader.forceLoadClass(classname);
                    target = Class.forName(classname, true, loader);
                }
            } catch (ClassNotFoundException e) {
                throw new BuildException(
                    "Could not find %s. Make sure you have it in your classpath",
                    classname);
            }
            main = target.getMethod("main", String[].class);
            if (main == null) {
                throw new BuildException("Could not find main() method in %s",
                    classname);
            }
            if ((main.getModifiers() & Modifier.STATIC) == 0) {
                throw new BuildException(
                    "main() method in %s is not declared static", classname);
            }
            if (timeout == null) {
                run(); //NOSONAR
            } else {
                thread = new Thread(this, "ExecuteJava");
                Task currentThreadTask
                    = project.getThreadTask(Thread.currentThread());
                // TODO is the following really necessary? it is in the same thread group...
                project.registerThreadTask(thread, currentThreadTask);
                // if we run into a timeout, the run-away thread shall not
                // make the VM run forever - if no timeout occurs, Ant's
                // main thread will still be there to let the new thread
                // finish
                thread.setDaemon(true);
                Watchdog w = new Watchdog(timeout);
                w.addTimeoutObserver(this);
                synchronized (this) {
                    thread.start();
                    w.start();
                    try {
                        while (!done) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    if (timedOut) {
                        project.log("Timeout: sub-process interrupted",
                                    Project.MSG_WARN);
                    } else {
                        thread = null;
                        w.stop();
                    }
                }
            }
            if (caught != null) {
                throw caught;
            }
        } catch (BuildException | ThreadDeath | SecurityException e) {
            throw e;
        } catch (Throwable e) {
            throw new BuildException(e);
        } finally {
            if (loader != null) {
                loader.resetThreadContextLoader();
                loader.cleanup();
                loader = null;
            }
            if (sysProperties != null) {
                sysProperties.restoreSystem();
            }
        }
    }

    /**
     * Run this ExecuteJava in a Thread.
     * @since Ant 1.5
     */
    @Override
    public void run() {
        final Object[] argument = {javaCommand.getArguments()};
        try {
            if (perm != null) {
                perm.setSecurityManager();
            }
            main.invoke(null, argument);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!(t instanceof InterruptedException)) {
                caught = t;
            } /* else { swallow, probably due to timeout } */
        } catch (Throwable t) {
            caught = t;
        } finally {
            if (perm != null) {
                perm.restoreSecurityManager();
            }
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }
    }

    /**
     * Mark timeout as having occurred.
     * @param w the responsible Watchdog.
     * @since Ant 1.5
     */
    @Override
    public synchronized void timeoutOccured(Watchdog w) {
        if (thread != null) {
            timedOut = true;
            thread.interrupt();
        }
        done = true;
        notifyAll();
    }

    /**
     * Get whether the process was killed.
     * @return <code>true</code> if the process was killed, false otherwise.
     * @since 1.19, Ant 1.5
     */
    public synchronized boolean killedProcess() {
        return timedOut;
    }

    /**
     * Run the Java command in a separate VM, this does not give you
     * the full flexibility of the Java task, but may be enough for
     * simple needs.
     * @param pc the ProjectComponent to use for logging, etc.
     * @return the exit status of the subprocess.
     * @throws BuildException on error.
     * @since Ant 1.6.3
     */
    public int fork(ProjectComponent pc) throws BuildException {
        CommandlineJava cmdl = new CommandlineJava();
        cmdl.setClassname(javaCommand.getExecutable());
        for (String arg : javaCommand.getArguments()) {
            cmdl.createArgument().setValue(arg);
        }
        if (classpath != null) {
            cmdl.createClasspath(pc.getProject()).append(classpath);
        }
        if (sysProperties != null) {
            cmdl.addSysproperties(sysProperties);
        }
        Redirector redirector = new Redirector(pc);
        Execute exe
            = new Execute(redirector.createHandler(),
                          timeout == null
                          ? null
                          : new ExecuteWatchdog(timeout));
        exe.setAntRun(pc.getProject());
        if (Os.isFamily("openvms")) {
            setupCommandLineForVMS(exe, cmdl.getCommandline());
        } else {
            exe.setCommandline(cmdl.getCommandline());
        }
        try {
            int rc = exe.execute();
            redirector.complete();
            return rc;
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            timedOut = exe.killedProcess();
        }
    }

    /**
     * On VMS platform, we need to create a special java options file
     * containing the arguments and classpath for the java command.
     * The special file is supported by the "-V" switch on the VMS JVM.
     *
     * @param exe the Execute instance to alter.
     * @param command the command-line.
     */
    public static void setupCommandLineForVMS(Execute exe, String[] command) {
        //Use the VM launcher instead of shell launcher on VMS
        exe.setVMLauncher(true);
        File vmsJavaOptionFile = null;
        try {
            String[] args = new String[command.length - 1];
            System.arraycopy(command, 1, args, 0, command.length - 1);
            vmsJavaOptionFile = JavaEnvUtils.createVmsJavaOptionFile(args);
            //we mark the file to be deleted on exit.
            //the alternative would be to cache the filename and delete
            //after execution finished, which is much better for long-lived runtimes
            //though spawning complicates things...
            vmsJavaOptionFile.deleteOnExit();
            String[] vmsCmd = {command[0], "-V", vmsJavaOptionFile.getPath()};
            exe.setCommandline(vmsCmd);
        } catch (IOException e) {
            throw new BuildException("Failed to create a temporary file for \"-V\" switch");
        }
    }

}
