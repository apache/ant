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

import org.apache.tools.ant.BuildException;

/**
 * Destroys a process running for too long.
 * For example:
 * <pre>
 * ExecuteWatchdog watchdog = new ExecuteWatchdog(30000);
 * Execute exec = new Execute(myloghandler, watchdog);
 * exec.setCommandLine(mycmdline);
 * int exitvalue = exec.execute();
 * if (exitvalue != SUCCESS && watchdog.killedProcess()){
 *              // it was killed on purpose by the watchdog
 * }
 * </pre>
 
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @see Execute
 */
public class ExecuteWatchdog implements Runnable {
        
    /** the process to execute and watch for duration */
    private Process process;

    /** timeout duration. Once the process running time exceeds this it should be killed */
    private int timeout;

    /** say whether or not the watchog is currently monitoring a process */
    private boolean watch = false;
        
    /** exception that might be thrown during the process execution */
    private Exception caught = null;

    /** say whether or not the process was killed due to running overtime */
    private boolean     killedProcess = false;

    /**
     * Creates a new watchdog with a given timeout.
     *
     * @param timeout the timeout for the process in milliseconds. It must be greather than 0.
     */
    public ExecuteWatchdog(int timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException("timeout lesser than 1.");
        }
        this.timeout = timeout;
    }

    /**
     * Watches the given process and terminates it, if it runs for too long.
     * All information from the previous run are reset.
     * @param process the process to monitor. It cannot be <tt>null</tt>
     * @throws IllegalStateException    thrown if a process is still being monitored.
     */
    public synchronized void start(Process process) {
        if (process == null) {
            throw new NullPointerException("process is null.");
        }
        if (this.process != null) {
            throw new IllegalStateException("Already running.");
        }
        this.caught = null;
        this.killedProcess = false;
        this.watch = true;
        this.process = process;
        final Thread thread = new Thread(this, "WATCHDOG");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the watcher. It will notify all threads possibly waiting on this object.
     */
    public synchronized void stop() {
        watch = false;
        notifyAll();
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

            // if we are here, either someone stopped the watchdog,
            // we are on timeout and the process must be killed, or
            // we are on timeout and the process has already stopped.
            try {
                // We must check if the process was not stopped
                // before being here
                process.exitValue();
            } catch (IllegalThreadStateException e){
                // the process is not terminated, if this is really
                // a timeout and not a manual stop then kill it.
                if (watch){
                    killedProcess = true;
                    process.destroy();
                }
            }
        } catch(Exception e) {
            caught = e;
        } finally {
            cleanUp();
        }
    }

    /**
     * reset the monitor flag and the process.
     */
    protected void cleanUp() {
        watch = false;
        process = null;
    }

    /**
     * This method will rethrow the exception that was possibly caught during the
     * run of the process. It will only remains valid once the process has been
     * terminated either by 'error', timeout or manual intervention. Information
     * will be discarded once a new process is ran.
     * @throws  BuildException  a wrapped exception over the one that was silently
     * swallowed and stored during the process run.
     */
    public void checkException() throws BuildException {
        if (caught != null) {
            throw new BuildException("Exception in ExecuteWatchdog.run: "
                                     + caught.getMessage(), caught);
        }
    }

    /**
     * Indicates whether or not the watchdog is still monitoring the process.
     * @return  <tt>true</tt> if the process is still running, otherwise <tt>false</tt>.
     */
    public boolean isWatching(){
        return watch;
    }

    /**
     * Indicates whether the last process run was killed on timeout or not.
     * @return  <tt>true</tt> if the process was killed otherwise <tt>false</tt>.
     */
    public boolean killedProcess(){
        return killedProcess;
    }
}

