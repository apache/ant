/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import java.util.*;
import java.text.*;
import java.lang.RuntimeException;

/**
 * Implements a multi threaded task execution.
 * <p>
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 * @author <a href="mailto:conor@apache.org">Conor MacNeill </a>
 */
public class Parallel extends Task
                      implements TaskContainer {

    /** Collection holding the nested tasks */
    private Vector nestedTasks = new Vector();


    /**
     * Add a nested task to execute parallel (asynchron).
     * <p>
     * @param nestedTask  Nested task to be executed in parallel
     */
    public void addTask(Task nestedTask) throws BuildException {
        nestedTasks.addElement(nestedTask);
    }

    /**
     * Block execution until the specified time or for a
     * specified amount of milliseconds and if defined,
     * execute the wait status.
     */
    public void execute() throws BuildException {
        TaskThread[] threads = new TaskThread[nestedTasks.size()];
        int threadNumber = 0;
        for (Enumeration e = nestedTasks.elements(); e.hasMoreElements(); threadNumber++) {
            Task nestedTask = (Task)e.nextElement();
            threads[threadNumber] = new TaskThread(threadNumber, nestedTask);
        }

        // now start all threads        
        for (int i = 0; i < threads.length; ++i) {
            threads[i].start();
        }

        // now join to all the threads 
        for (int i = 0; i < threads.length; ++i) {
            try {
                threads[i].join();
            }
            catch (InterruptedException ie) {
                // who would interrupt me at a time like this?
            }
        }
        
        // now did any of the threads throw an exception
        StringBuffer exceptionMessage = new StringBuffer();
        String lSep = System.getProperty("line.separator");
        int numExceptions = 0;
        Throwable firstException = null;
        Location firstLocation = Location.UNKNOWN_LOCATION;;
        for (int i = 0; i < threads.length; ++i) {
            Throwable t = threads[i].getException();
            if (t != null) {
                numExceptions++;
                if (firstException == null) {
                    firstException = t;
                }
                if (t instanceof BuildException && 
                        firstLocation == Location.UNKNOWN_LOCATION) {
                    firstLocation = ((BuildException)t).getLocation();
                }
                exceptionMessage.append(lSep);
                exceptionMessage.append(t.getMessage());
            }
        }
        
        if (numExceptions == 1) {
            if (firstException instanceof BuildException) {
                throw (BuildException)firstException;
            }
            else {
                throw new BuildException(firstException);
            }
        }
        else if (numExceptions > 1) {
            throw new BuildException(exceptionMessage.toString(), firstLocation);
        }
    }

    class TaskThread extends Thread {
        private Throwable exception;
        private Task task;
        private int taskNumber;

        /**
         * Construct a new TaskThread<p>
         *
         * @param task the Task to be executed in a seperate thread
         */
        TaskThread(int taskNumber, Task task) {
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
            }
            catch (Throwable t) {
                exception = t;
            }
        }
        
        public Throwable getException() { 
            return exception;
        }
    }
}
