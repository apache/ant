/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

/**
 * Destroys a process running for too long.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class ExecuteWatchdog implements Runnable {

    private Process process;
    private int timeout;
    private boolean watch = true;
    private Exception caught = null;


    /**
     * Creates a new watchdog.
     *
     * @param timeout the timeout for the process.
     */
    public ExecuteWatchdog(int timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException("timeout lesser than 1.");
        }
        this.timeout = timeout;
    }


    /**
     * Watches the given process and terminates it, if it runs for to long.
     *
     * @param process the process to watch.
     */
    public synchronized void start(Process process) {
        if (process == null) {
            throw new NullPointerException("process is null.");
        }
        if (this.process != null) {
            throw new IllegalStateException("Already running.");
        }
        watch = true;
        this.process = process;
        final Thread thread = new Thread(this, "WATCHDOG");
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Stops the watcher.
     */
    public synchronized void stop() {
        watch = false;
        notifyAll();
        process = null;
    }


    /**
     * Watches the process and terminates it, if it runs for to long.
     */
    public synchronized void run() {
	try {
	    // This isn't a Task, don't have a Project object to log.
	    // project.log("ExecuteWatchdog: timeout = "+timeout+" msec",  Project.MSG_VERBOSE);
	    final long until = System.currentTimeMillis() + timeout;
	    long now;
	    while (watch && until > (now = System.currentTimeMillis())) {
		try {
		    wait(until - now);
		} catch (InterruptedException e) {}
	    }
	    if (watch) {
		process.destroy();
	    }
	    stop();
	} catch(Exception e) {
            caught = e;
        }
    }

    public void checkException() throws BuildException {
        if (caught != null) {
            throw new BuildException("Exception in ExecuteWatchdog.run: "
                                     + caught.getMessage(), caught);
        }
    }
}

