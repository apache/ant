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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.property.LocalProperties;

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

    private static final int NUMBER_TRIES = 100;

    /** Class which holds a list of tasks to execute */
    public static class TaskList implements TaskContainer {
        /** Collection holding the nested tasks */
        private List<Task> tasks = new ArrayList<>();

        /**
         * Add a nested task to execute in parallel (asynchronously).
         * <p>
         * @param nestedTask  Nested task to be executed in parallel.
         *                    must not be null.
         */
        @Override
        public void addTask(Task nestedTask) {
            tasks.add(nestedTask);
        }
    }

    /** Collection holding the nested tasks */
    private Vector<Task> nestedTasks = new Vector<>();

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

    /** The daemon task list if any */
    private TaskList daemonTasks;

    /** Accumulation of exceptions messages from all nested tasks */
    private StringBuffer exceptionMessage;

    /** Number of exceptions from nested tasks */
    private int numExceptions = 0;

    /** The first exception encountered */
    private Throwable firstException;

    /** The location of the first exception */
    private Location firstLocation;

    /** The status of the first ExitStatusException. */
    private Integer firstExitStatus;

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
    @Override
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * Dynamically generates the number of threads to execute based on the
     * number of available processors (via
     * <code>java.lang.Runtime.availableProcessors()</code>).
     * Will overwrite the value set in threadCount; optional
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
     * is set then this value is
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
    @Override
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
            numThreads = Runtime.getRuntime().availableProcessors()
                    * numThreadsPerProcessor;
        }
    }

    private void processExceptions(TaskRunnable[] runnables) {
        if (runnables == null) {
            return;
        }
        for (TaskRunnable runnable : runnables) {
            Throwable t = runnable.getException();
            if (t != null) {
                numExceptions++;
                if (firstException == null) {
                    firstException = t;
                }
                if (t instanceof BuildException
                    && firstLocation == Location.UNKNOWN_LOCATION) {
                    firstLocation = ((BuildException) t).getLocation();
                }
                if (t instanceof ExitStatusException
                    && firstExitStatus == null) {
                    ExitStatusException ex = (ExitStatusException) t;
                    firstExitStatus = ex.getStatus();
                    // potentially overwriting existing value but the
                    // location should match the exit status
                    firstLocation = ex.getLocation();
                }
                exceptionMessage.append(System.lineSeparator());
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
        stillRunning = true;
        timedOut = false;
        boolean interrupted = false;

        TaskRunnable[] runnables = nestedTasks.stream().map(TaskRunnable::new)
            .toArray(TaskRunnable[]::new);

        final int numTasks = nestedTasks.size();
        final int maxRunning = numTasks < numThreads ? numTasks : numThreads;

        TaskRunnable[] running = new TaskRunnable[maxRunning];
        ThreadGroup group = new ThreadGroup("parallel");

        TaskRunnable[] daemons = null;
        if (daemonTasks != null && !daemonTasks.tasks.isEmpty()) {
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
                    daemons[i] = new TaskRunnable(daemonTasks.tasks.get(i));
                    Thread daemonThread =  new Thread(group, daemons[i]);
                    daemonThread.setDaemon(true);
                    daemonThread.start();
                }
            }

            // now run main threads in limited numbers...
            // start initial batch of threads
            int threadNumber = 0;
            for (int i = 0; i < maxRunning; ++i) {
                running[i] = runnables[threadNumber++];
                Thread thread =  new Thread(group, running[i]);
                thread.start();
            }

            if (timeout != 0) {
                // start the timeout thread
                Thread timeoutThread = new Thread() {
                    @Override
                    public synchronized void run() {
                        try {
                            final long start = System.currentTimeMillis();
                            final long end = start + timeout;
                            long now = System.currentTimeMillis();
                            while (now < end) {
                                wait(end - now);
                                now = System.currentTimeMillis();
                            }
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

            try {
                // now find available running slots for the remaining threads
                outer: while (threadNumber < numTasks && stillRunning) {
                    for (int i = 0; i < maxRunning; i++) {
                        if (running[i] == null || running[i].isFinished()) {
                            running[i] = runnables[threadNumber++];
                            Thread thread = new Thread(group, running[i]);
                            thread.start();
                            // continue on outer while loop to get another
                            // available slot
                            continue outer;
                        }
                    }

                    // if we got here all slots in use, so sleep until
                    // something happens
                    semaphore.wait();
                }

                // are all threads finished
                outer2: while (stillRunning) {
                    for (int i = 0; i < maxRunning; ++i) {
                        if (running[i] != null && !running[i].isFinished()) {
                            // System.out.println("Thread " + i + " is still
                            // alive ");
                            // still running - wait for it
                            semaphore.wait(); //NOSONAR
                            continue outer2;
                        }
                    }
                    stillRunning = false;
                }
            } catch (InterruptedException ie) {
                interrupted = true;
            }

            if (!timedOut && !failOnAny) {
                // https://issues.apache.org/bugzilla/show_bug.cgi?id=49527
                killAll(running);
            }
        }

        if (interrupted) {
            throw new BuildException("Parallel execution interrupted.");
        }
        if (timedOut) {
            throw new BuildException("Parallel execution timed out");
        }

        // now did any of the threads throw an exception
        exceptionMessage = new StringBuffer();
        numExceptions = 0;
        firstException = null;
        firstExitStatus = null;
        firstLocation = Location.UNKNOWN_LOCATION;
        processExceptions(daemons);
        processExceptions(runnables);

        if (numExceptions == 1) {
            if (firstException instanceof BuildException) {
                throw (BuildException) firstException;
            }
            throw new BuildException(firstException);
        }
        if (numExceptions > 1) {
            if (firstExitStatus == null) {
                throw new BuildException(exceptionMessage.toString(),
                                         firstLocation);
            }
            throw new ExitStatusException(exceptionMessage.toString(),
                                          firstExitStatus, firstLocation);
        }
    }

    /**
     * Doesn't do anything if all threads where already gone,
     * else it tries to interrupt the threads 100 times.
     * @param running The list of tasks that may currently be running.
     */
    private void killAll(TaskRunnable[] running) {
        boolean oneAlive;
        int tries = 0;
        do {
            oneAlive = false;
            for (TaskRunnable runnable : running) {
                if (runnable != null && !runnable.isFinished()) {
                    runnable.interrupt();
                    Thread.yield();
                    oneAlive = true;
                }
            }
            if (oneAlive) {
                tries++;
                Thread.yield();
            }
        } while (oneAlive && tries < NUMBER_TRIES);
    }

    /**
     * thread that execs a task
     */
    private class TaskRunnable implements Runnable {
        private Throwable exception;
        private Task task;
        private boolean finished;
        private volatile Thread thread;

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
        @Override
        public void run() {
            try {
                LocalProperties.get(getProject()).copy();
                thread = Thread.currentThread();
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

        void interrupt() {
            thread.interrupt();
        }
    }

}
