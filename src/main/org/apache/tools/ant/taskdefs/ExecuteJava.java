/* 
 * Copyright  2000-2004 Apache Software Foundation
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


package org.apache.tools.ant.taskdefs;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.util.TimeoutObserver;
import org.apache.tools.ant.util.Watchdog;

/**
 *
 * @author thomas.haas@softwired-inc.com
 * @author Stefan Bodewig
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 * @since Ant 1.2
 */
public class ExecuteJava implements Runnable, TimeoutObserver {

    private Commandline javaCommand = null;
    private Path classpath = null;
    private CommandlineJava.SysProperties sysProperties = null;
    private Permissions  perm = null;
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
     * Permissions for the application run.
     * @since Ant 1.6
     * @param permissions
     */
    public void setPermissions(Permissions permissions) {
        perm = permissions;
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
     * @since Ant 1.5
     */
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public void execute(Project project) throws BuildException {
        final String classname = javaCommand.getExecutable();

        AntClassLoader loader = null;
        try {
            if (sysProperties != null) {
                sysProperties.setSystem();
            }

            final Class[] param = {Class.forName("[Ljava.lang.String;")};
            Class target = null;
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
            main = target.getMethod("main", param);
            if (main == null) {
                throw new BuildException("Could not find main() method in "
                                         + classname);
            }

            if ((main.getModifiers() & Modifier.STATIC) == 0) {
                throw new BuildException("main() method in " + classname
                    + " is not declared static");
            }


            if (timeout == null) {
                run();
            } else {
                thread = new Thread(this, "ExecuteJava");
                Task currentThreadTask
                    = project.getThreadTask(Thread.currentThread());
                project.registerThreadTask(thread, currentThreadTask);
                // if we run into a timeout, the run-away thread shall not
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
     * @since Ant 1.5
     */
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
                notifyAll();
            }
        }
    }

    /**
     * @since Ant 1.5
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
    public synchronized boolean killedProcess() {
        return timedOut;
    }
}