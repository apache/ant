/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.TimeoutObserver;
import org.apache.tools.ant.util.Watchdog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.PrintStream;

/**
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @since Ant 1.2
 */
public class ExecuteJava implements Runnable, TimeoutObserver {

    private Commandline javaCommand = null;
    private Path classpath = null;
    private CommandlineJava.SysProperties sysProperties = null;
    private Method main = null;
    private Long timeout = null;
    private Throwable caught = null;
    private boolean timedOut = false;
    private Thread thread = null;

    public void setJavaCommand(Commandline javaCommand) {
        this.javaCommand = javaCommand;
    }

    /**
     * Set the classpath to be used when running the Java class
     * 
     * @param p an Ant Path object containing the classpath.
     */
    public void setClasspath(Path p) {
        classpath = p;
    }

    public void setSystemProperties(CommandlineJava.SysProperties s) {
        sysProperties = s;
    }

    /**
     * All output (System.out as well as System.err) will be written
     * to this Stream.
     *
     * @deprecated manage output at the task level
     */
    public void setOutput(PrintStream out) {
    }

    /**
     * @since 1.19, Ant 1.5
     */
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public void execute(Project project) throws BuildException{
        final String classname = javaCommand.getExecutable();

        AntClassLoader loader = null; 
        try {
            if (sysProperties != null) {
                sysProperties.setSystem();
            }

            final Class[] param = { Class.forName("[Ljava.lang.String;") };
            Class target = null;
            if (classpath == null) {
                target = Class.forName(classname);
            } else {
                loader = new AntClassLoader(project.getCoreLoader(), project, 
                                            classpath, false);
                loader.setIsolated(true);
                loader.setThreadContextLoader();
                target = loader.forceLoadClass(classname);
                AntClassLoader.initializeClass(target);
            }
            main = target.getMethod("main", param);
            if (main == null) {
                throw new BuildException("Could not find main() method in " 
                                         + classname);
            }

            if (timeout == null) {
                run();
            } else {
                thread = new Thread(this, "ExecuteJava");
                Task currentThreadTask 
                    = project.getThreadTask(Thread.currentThread());
                project.registerThreadTask(thread, currentThreadTask);
                // if we run into a timout, the run-away thread shall not
                // make the VM run forever - if no timeout occurs, Ant's
                // main thread will still be there to let the new thread
                // finish
                thread.setDaemon(true);
                Watchdog w = new Watchdog(timeout.longValue());
                w.addTimeoutObserver(this);
                synchronized (this) {
                    thread.start();
                    w.start();
                    try {
                        wait();
                    } catch (InterruptedException e) {}
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

        } catch (ClassNotFoundException e) {
            throw new BuildException("Could not find " + classname + "."
                                     + " Make sure you have it in your"
                                     + " classpath");
        } catch (SecurityException e) {
            throw e;
        } catch (Throwable e) {
            throw new BuildException(e);
        } finally {
            if (loader != null) {
                loader.resetThreadContextLoader();
                loader.cleanup();
            }
            if (sysProperties != null) {
                sysProperties.restoreSystem();
            }
        }
    }

    /**
     * @since 1.19, Ant 1.5
     */
    public void run() {
        final Object[] argument = { javaCommand.getArguments() };
        try {
            main.invoke(null, argument);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!(t instanceof InterruptedException)) {
                caught = t;
            } /* else { swallow, probably due to timeout } */
        } catch (Throwable t) {
            caught = t;
        } finally {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * @since 1.19, Ant 1.5
     */
    public synchronized void timeoutOccured(Watchdog w) {
        if (thread != null) {
            timedOut = true;
            thread.interrupt();
        }
        notifyAll();
    }

    /**
     * @since 1.19, Ant 1.5
     */
    public boolean killedProcess() {
        return timedOut;
    }
}
