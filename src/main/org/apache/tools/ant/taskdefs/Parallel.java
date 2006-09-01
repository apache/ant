/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.util.StringUtils;

/**
 * Executes the contained tasks in separate threads, continuing
 * once all are completed.
 * <p>
 * New behavior allows for the ant script to specify a maximum number of
 * threads that will be executed in parallel.  One should be very careful about
 * using the <code>waitFor</code> task when specifying <code>threadCount</code>
 * as it can cause deadlocks if the number of threads is too small or if one of
 * the nested tasks fails to execute completely.  The task selection algorithm
 * will insure that the tasks listed before a task have started before that
 * task is started, but it will not insure a successful completion of those
 * tasks or that those tasks will finish first (i.e. it's a classic race
 * condition).
 * </p>
 * @since Ant 1.4
 *
 * @ant.task category="control"
 */
public class Parallel extends Task
                      implements TaskContainer {

    /** Class which holds a list of tasks to execute */
    public static class TaskList implements TaskContainer {
        /** Collection holding the nested tasks */
        private List tasks = new ArrayList();

        /**
         * Add a nested task to execute parallel (asynchron).
         * <p>
         * @param nestedTask  Nested task to be executed in parallel.
         *                    must not be null.
         */
        public void addTask(Task nestedTask) {
            tasks.add(nestedTask);
        }
    }

    /** Collection holding the nested tasks */
    private Vector nestedTasks = new Vector();

    /** Semaphore to notify of completed threads */
    private final Object semaphore = new Object();

    /** Total number of threads to run */
    private int numThreads = 0;

    /** Total number of threads per processor to run.  */
    private int numThreadsPerProcessor = 0;

    /** The timeout period in milliseconds */
    private long timeout;

    /** Indicates threads are still running and new threads can be issued */
    private volatile boolean stillRunning;

    /** Indicates that the execution timedout */
    private boolean timedOut;

    /**
     * Indicates whether failure of any of the nested tasks should end
     * execution
     */
    private boolean failOnAny;

    /** The dameon task list if any */
    private TaskList daemonTasks;

    /** Accumulation of exceptions messages from all nested tasks */
    private StringBuffer exceptionMessage;

    /** Number of exceptions from nested tasks */
    private int numExceptions = 0;

    /** The first exception encountered */
    private Throwable firstException;

    /** The location of the first exception */
    private Location firstLocation;

    /**
     * Add a group of daemon threads
     * @param daemonTasks The tasks to be executed as daemon.
     */
    public void addDaemons(TaskList daemonTasks) {
        if (this.daemonTasks != null) {
            throw new BuildException("Only one daemon group is supported");
        }
        this.daemonTasks = daemonTasks;
    }

    /**
     * Interval to poll for completed threads when threadCount or
     * threadsPerProcessor is specified.  Integer in milliseconds.; optional
     *
     * @param pollInterval New value of property pollInterval.
     */
    public void setPollInterval(int pollInterval) {
    }

    /**
     * Control whether a failure in a nested task halts execution. Note that
     * the task will complete but existing threads will continue to run - they
     * are not stopped
     *
     * @param failOnAny if true any nested task failure causes parallel to
     *        complete.
     */
    public void setFailOnAny(boolean failOnAny) {
        this.failOnAny = failOnAny;
    }

    /**
     * Add a nested task to execute in parallel.
     * @param nestedTask  Nested task to be executed in parallel
     */
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * Dynamically generates the number of threads to execute based on the
     * number of available processors (via
     * <code>java.lang.Runtime.availableProcessors()</code>). Requires a J2SE
     * 1.4 VM, and it will overwrite the value set in threadCount.
     * If used in a 1.1, 1.2, or 1.3 VM then the task will defer to
     * <code>threadCount</code>.; optional
     * @param numThreadsPerProcessor Number of threads to create per available
     *        processor.
     *
     */
    public void setThreadsPerProcessor(int numThreadsPerProcessor) {
        this.numThreadsPerProcessor = numThreadsPerProcessor;
    }

    /**
     * Statically determine the maximum number of tasks to execute
     * simultaneously.  If there are less tasks than threads then all will be
     * executed at once, if there are more then only <code>threadCount</code>
     * tasks will be executed at one time.  If <code>threadsPerProcessor</code>
     * is set and the JVM is at least a 1.4 VM then this value is
     * ignored.; optional
     *
     * @param numThreads total number of threads.
     *
     */
    public void setThreadCount(int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * Sets the timeout on this set of tasks. If the timeout is reached
     * before the other threads complete, the execution of this
     * task completes with an exception.
     *
     * Note that existing threads continue to run.
     *
     * @param timeout timeout in milliseconds.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }



    /**
     * Execute the parallel tasks
     *
     * @exception BuildException if any of the threads failed.
     */
    public void execute() throws BuildException {
        updateThreadCounts();
        if (numThreads == 0) {
            numThreads = nestedTasks.size();
        }
        spinThreads();
    }

    /**
     * Determine the number of threads based on the number of processors
     */
    private void updateThreadCounts() {
        if (numThreadsPerProcessor != 0) {
            int numProcessors = getNumProcessors();
            if (numProcessors != 0) {
                numThreads = numProcessors * numThreadsPerProcessor;
            }
        }
    }

    private void processExceptions(TaskRunnable[] runnables) {
        if (runnables == null) {
            return;
        }
        for (int i = 0; i < runnables.length; ++i) {
            Throwable t = runnables[i].getException();
            if (t != null) {
                numExceptions++;
                if (firstException == null) {
                    firstException = t;
                }
                if (t instanceof BuildException
                    && firstLocation == Location.UNKNOWN_LOCATION) {
                    firstLocation = ((BuildException) t).getLocation();
                }
                exceptionMessage.append(StringUtils.LINE_SEP);
                exceptionMessage.append(t.getMessage());
            }
        }
    }

    /**
     * Spin up required threads with a maximum number active at any given time.
     *
     * @exception BuildException if any of the threads failed.
     */
    private void spinThreads() throws BuildException {
        final int numTasks = nestedTasks.size();
        TaskRunnable[] runnables = new TaskRunnable[numTasks];
        stillRunning = true;
        timedOut = false;

        int threadNumber = 0;
        for (Enumeration e = nestedTasks.elements(); e.hasMoreElements();
             threadNumber++) {
            Task nestedTask = (Task) e.nextElement();
            runnables[threadNumber]
                = new TaskRunnable(nestedTask);
        }

        final int maxRunning = numTasks < numThreads ? numTasks : numThreads;
        TaskRunnable[] running = new TaskRunnable[maxRunning];

        threadNumber = 0;
        ThreadGroup group = new ThreadGroup("parallel");

        TaskRunnable[] daemons = null;
        if (daemonTasks != null && daemonTasks.tasks.size() != 0) {
            daemons = new TaskRunnable[daemonTasks.tasks.size()];
        }

        synchronized (semaphore) {
            // When we leave this block we can be sure all data is really
            // stored in main memory before the new threads start, the new
            // threads will for sure load the data from main memory.
            //
            // This probably is slightly paranoid.
        }

        synchronized (semaphore) {
            // start any daemon threads
            if (daemons != null) {
                for (int i = 0; i < daemons.length; ++i) {
                    daemons[i] = new TaskRunnable((Task) daemonTasks.tasks.get(i));
                    Thread daemonThread =  new Thread(group, daemons[i]);
                    daemonThread.setDaemon(true);
                    daemonThread.start();
                }
            }

            // now run main threads in limited numbers...
            // start initial batch of threads
            for (int i = 0; i < maxRunning; ++i) {
                running[i] = runnables[threadNumber++];
                Thread thread =  new Thread(group, running[i]);
                thread.start();
            }

            if (timeout != 0) {
                // start the timeout thread
                Thread timeoutThread = new Thread() {
                    public synchronized void run() {
                        try {
                            wait(timeout);
                            synchronized (semaphore) {
                                stillRunning = false;
                                timedOut = true;
                                semaphore.notifyAll();
                            }
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                };
                timeoutThread.start();
            }

            // now find available running slots for the remaining threads
            outer:
            while (threadNumber < numTasks && stillRunning) {
                for (int i = 0; i < maxRunning; i++) {
                    if (running[i] == null || running[i].isFinished()) {
                        running[i] = runnables[threadNumber++];
                        Thread thread =  new Thread(group, running[i]);
                        thread.start();
                        // continue on outer while loop to get another
                        // available slot
                        continue outer;
                    }
                }

                // if we got here all slots in use, so sleep until
                // something happens
                try {
                    semaphore.wait();
                } catch (InterruptedException ie) {
                    // doesn't java know interruptions are rude?
                    // just pretend it didn't happen and go about out business.
                    // sheesh!
                }
            }

            // are all threads finished
            outer2:
            while (stillRunning) {
                for (int i = 0; i < maxRunning; ++i) {
                    if (running[i] != null && !running[i].isFinished()) {
                        //System.out.println("Thread " + i + " is still alive ");
                        // still running - wait for it
                        try {
                            semaphore.wait();
                        } catch (InterruptedException ie) {
                            // who would interrupt me at a time like this?
                        }
                        continue outer2;
                    }
                }
                stillRunning = false;
            }
        }

        if (timedOut) {
            throw new BuildException("Parallel execution timed out");
        }

        // now did any of the threads throw an exception
        exceptionMessage = new StringBuffer();
        numExceptions = 0;
        firstException = null;
        firstLocation = Location.UNKNOWN_LOCATION;
        processExceptions(daemons);
        processExceptions(runnables);

        if (numExceptions == 1) {
            if (firstException instanceof BuildException) {
                throw (BuildException) firstException;
            } else {
                throw new BuildException(firstException);
            }
        } else if (numExceptions > 1) {
            throw new BuildException(exceptionMessage.toString(),
                                     firstLocation);
        }
    }

    /**
     * Determine the number of processors. Only effective on later VMs
     *
     * @return the number of processors available or 0 if not determinable.
     */
    private int getNumProcessors() {
        try {
            Class[] paramTypes = {};
            Method availableProcessors =
                Runtime.class.getMethod("availableProcessors", paramTypes);

            Object[] args = {};
            Integer ret = (Integer) availableProcessors.invoke(Runtime.getRuntime(), args);
            return ret.intValue();
        } catch (Exception e) {
            // return a bogus number
            return 0;
        }
    }

    /**
     * thread that execs a task
     */
    private class TaskRunnable implements Runnable {
        private Throwable exception;
        private Task task;
        private boolean finished;

        /**
         * Construct a new TaskRunnable.<p>
         *
         * @param task the Task to be executed in a separate thread
         */
        TaskRunnable(Task task) {
            this.task = task;
        }

        /**
         * Executes the task within a thread and takes care about
         * Exceptions raised within the task.
         */
        public void run() {
            try {
                task.perform();
            } catch (Throwable t) {
                exception = t;
                if (failOnAny) {
                    stillRunning = false;
                }
            } finally {
                synchronized (semaphore) {
                    finished = true;
                    semaphore.notifyAll();
                }
            }
        }

        /**
         * get any exception that got thrown during execution;
         * @return an exception or null for no exception/not yet finished
         */
        public Throwable getException() {
            return exception;
        }

        /**
         * Provides the indicator that the task has been finished.
         * @return Returns true when the task is finished.
         */
        boolean isFinished() {
            return finished;
        }
    }

}
