/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.util.StringUtils;

/**
 * Executes the contained tasks in separate threads, continuing
 * once all are completed.<br/>
 * New behavior allows for the ant script to specify a maximum number of 
 * threads that will be executed in parallel.  One should be very careful about
 * using the <code>waitFor</code> task when specifying <code>threadCount</code>
 * as it can cause deadlocks if the number of threads is too small or if one of 
 * the nested tasks fails to execute completely.  The task selection algorithm 
 * will insure that the tasks listed before a task have started before that 
 * task is started, but it will not insure a successful completion of those 
 * tasks or that those tasks will finish first (i.e. it's a classic race 
 * condition).
 * <p>
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 * @author Conor MacNeill
 * @author Danno Ferrin
 * @since Ant 1.4
 *
 * @ant.task category="control"
 */
public class Parallel extends Task
                      implements TaskContainer {

    /** Collection holding the nested tasks */
    private Vector nestedTasks = new Vector();

    /** Semaphore to notify of completed threads */
    private final Object semaphore = new Object();
    
    /** Total number of threads to run */
    int numThreads = 0;
    
    /** Total number of threads per processor to run.  */
    int numThreadsPerProcessor = 0;
    
    /** Interval (in ms) to poll for finished threads. */
    int pollInterval = 1000; // default is once a second

    /**
     * Add a nested task to execute in parallel.
     * @param nestedTask  Nested task to be executed in parallel
     */
    public void addTask(Task nestedTask) throws BuildException {
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
     * is set and the JVM is at least a 1.4 VM then this value is ignormed.; optional
     *
     * @param numThreads total number of therads.
     *
     */
    public void setThreadCount(int numThreads) {
        this.numThreads = numThreads;
    }

    /** 
     * Interval to poll for completed threads when threadCount or 
     * threadsPerProcessor is specified.  Integer in milliseconds.; optional
     *
     * @param pollInterval New value of property pollInterval.
     */
    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }
    
    public void execute() throws BuildException {
        updateThreadCounts();
        if (numThreads == 0) {
            numThreads = nestedTasks.size();
        }
        spinThreads();
    }
    
    private void updateThreadCounts() {
        if (numThreadsPerProcessor != 0) {
            int numProcessors = getNumProcessors();
            if (numProcessors != 0) {
                numThreads = numProcessors * numThreadsPerProcessor;
            }
        }
    }
        
    /**
     * Spin up threadCount threads.
     */
    private void spinThreads() throws BuildException {
        final int numTasks = nestedTasks.size();
        Thread[] threads = new Thread[numTasks];
        TaskRunnable[] runnables = new TaskRunnable[numTasks];
        int threadNumber = 0;
        for (Enumeration e = nestedTasks.elements(); e.hasMoreElements(); 
             threadNumber++) {
            Task nestedTask = (Task) e.nextElement();
            ThreadGroup group = new ThreadGroup("parallel");
            TaskRunnable taskRunnable 
                = new TaskRunnable(threadNumber, nestedTask);
            runnables[threadNumber] = taskRunnable;
            threads[threadNumber] = new Thread(group, taskRunnable);
        }

        final int maxRunning = numThreads;
        Thread[] running = new Thread[maxRunning];
        threadNumber = 0;
        
        // now run them in limited numbers...
        outer:
        while (threadNumber < numTasks) {
            synchronized(semaphore) {
                for (int i = 0; i < maxRunning; i++) {
                    if (running[i] == null || !running[i].isAlive()) {
                        running[i] = threads[threadNumber++];
                        running[i].start();
                        // countinue on outer while loop in case we used our last thread
                        continue outer;
                    }
                }
                // if we got here all are running, so sleep a little
                try {
                    semaphore.wait(pollInterval);
                } catch (InterruptedException ie) {
                    // dosen't java know interruptions are rude?
                    // just pretend it didn't happen and go aobut out business.
                    // sheesh!
                }
            }
        }
            
        // now join to all the threads 
        for (int i = 0; i < maxRunning; ++i) {
            try {
                if (running[i] != null) {
                    running[i].join();
                }
            } catch (InterruptedException ie) {
                // who would interrupt me at a time like this?
            }
        }
        
        // now did any of the threads throw an exception
        StringBuffer exceptionMessage = new StringBuffer();
        int numExceptions = 0;
        Throwable firstException = null;
        Location firstLocation = Location.UNKNOWN_LOCATION;;
        for (int i = 0; i < numTasks; ++i) {
            Throwable t = runnables[i].getException();
            if (t != null) {
                numExceptions++;
                if (firstException == null) {
                    firstException = t;
                }
                if (t instanceof BuildException && 
                        firstLocation == Location.UNKNOWN_LOCATION) {
                    firstLocation = ((BuildException) t).getLocation();
                }
                exceptionMessage.append(StringUtils.LINE_SEP);
                exceptionMessage.append(t.getMessage());
            }
        }
        
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
        private int taskNumber;

        /**
         * Construct a new TaskRunnable.<p>
         *
         * @param task the Task to be executed in a seperate thread
         */
        TaskRunnable(int taskNumber, Task task) {
            this.task = task;
            this.taskNumber = taskNumber;
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
            } finally {
                synchronized (semaphore) {
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
    }
    
}
