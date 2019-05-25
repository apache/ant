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

package org.apache.tools.ant.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * A worker ant executes a single task in a background thread.
 * After the run, any exception thrown is turned into a BuildException, which can be
 * rethrown, the finished attribute is set, then notifyAll() is called,
 * so that anyone waiting on the same notify object gets woken up.
 * <p>
 * This class is effectively a superset of
 * <code>org.apache.tools.ant.taskdefs.Parallel.TaskRunnable</code>
 *
 * @since Ant 1.8
 */

public class WorkerAnt extends Thread {

    private Task task;
    private Object notify;
    private volatile boolean finished = false;
    private volatile BuildException buildException;
    private volatile Throwable exception;

    /**
     * Error message if invoked with no task
     */
    public static final String ERROR_NO_TASK = "No task defined";


    /**
     * Create the worker.
     * <p>
     * This does not start the thread, merely configures it.
     * @param task the task
     * @param notify what to notify
     */
    public WorkerAnt(Task task, Object notify) {
        this.task = task;
        this.notify = notify != null ? notify : this;
    }

    /**
     * Create the worker, using the worker as the notification point.
     * <p>
     * This does not start the thread, merely configures it.
     * @param task the task
     */
    public WorkerAnt(Task task) {
        this(task, null);
    }

    /**
     * Get any build exception.
     * This would seem to be oversynchronised, but know that Java pre-1.5 can
     * reorder volatile access.
     * The synchronized attribute is to force an ordering.
     *
     * @return the exception or null
     */
    public synchronized BuildException getBuildException() {
        return buildException;
    }

    /**
     * Get whatever was thrown, which may or may not be a buildException.
     * Assertion: getException() instanceof BuildException &lt;=&gt; getBuildException()==getException()
     * @return the exception.
     */
    public synchronized Throwable getException() {
        return exception;
    }


    /**
     * Get the task
     * @return the task
     */
    public Task getTask() {
        return task;
    }


    /**
     * Query the task/thread for being finished.
     * This would seem to be oversynchronised, but know that Java pre-1.5 can
     * reorder volatile access.
     * The synchronized attribute is to force an ordering.
     * @return true if the task is finished.
     */
    public synchronized boolean isFinished() {
        return finished;
    }

    /**
     * Block on the notify object and so wait until the thread is finished.
     * @param timeout timeout in milliseconds
     * @throws InterruptedException if the execution was interrupted
     */
    public void waitUntilFinished(long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();
        final long end = start + timeout;
        synchronized (notify) {
            long now = System.currentTimeMillis();
            while (!finished && now < end) {
                notify.wait(end - now);
                now = System.currentTimeMillis();
            }
        }
    }

    /**
     * Raise an exception if one was caught
     *
     * @throws BuildException if one has been picked up
     */
    public void rethrowAnyBuildException() {
        BuildException ex = getBuildException();
        if (ex != null) {
            throw ex;
        }
    }


    /**
     * Handle a caught exception, by recording it and possibly wrapping it
     * in a BuildException for later rethrowing.
     * @param thrown what was caught earlier
     */
    private synchronized void caught(Throwable thrown) {
        exception = thrown;
        buildException = (thrown instanceof BuildException)
            ? (BuildException) thrown
            : new BuildException(thrown);
    }

    /**
     * Run the task, which is skipped if null.
     * When invoked again, the task is re-run.
     */
    public void run() {
        try {
            if (task != null) {
                task.execute();
            }
        } catch (Throwable thrown) {
            caught(thrown);
        } finally {
            synchronized (notify) {
                finished = true;
                //reset the task.
                //wake up our owner, if it is waiting
                notify.notifyAll();
            }
        }
    }
}
