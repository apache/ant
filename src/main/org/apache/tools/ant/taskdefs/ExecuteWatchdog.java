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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.TimeoutObserver;
import org.apache.tools.ant.util.Watchdog;

/**
 * Destroys a process running for too long.
 * For example:
 * <pre>
 * ExecuteWatchdog watchdog = new ExecuteWatchdog(30000);
 * Execute exec = new Execute(myloghandler, watchdog);
 * exec.setCommandLine(mycmdline);
 * int exitvalue = exec.execute();
 * if (Execute.isFailure(exitvalue) &amp;&amp; watchdog.killedProcess()) {
 *              // it was killed on purpose by the watchdog
 * }
 * </pre>

 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:sbailliez_at_apache.org">Stephane Bailliez</a>
 * @see Execute
 * @see org.apache.tools.ant.util.Watchdog
 * @since Ant 1.2
 */
public class ExecuteWatchdog implements TimeoutObserver {

    /** the process to execute and watch for duration */
    private Process process;

    /** say whether or not the watchdog is currently monitoring a process */
    private boolean watch = false;

    /** exception that might be thrown during the process execution */
    private Exception caught = null;

    /** say whether or not the process was killed due to running overtime */
    private boolean     killedProcess = false;

    /** will tell us whether timeout has occurred */
    private Watchdog watchdog;

    /**
     * Creates a new watchdog with a given timeout.
     *
     * @param timeout the timeout for the process in milliseconds.
     * It must be greater than 0.
     */
    public ExecuteWatchdog(long timeout) {
        watchdog = new Watchdog(timeout);
        watchdog.addTimeoutObserver(this);
    }

    /**
     * @see #ExecuteWatchdog(long)
     * @deprecated Use constructor with a long type instead.
     * (1.4.x compatibility)
     */
    public ExecuteWatchdog(int timeout) {
        this((long) timeout);
    }

    /**
     * Watches the given process and terminates it, if it runs for too long.
     * All information from the previous run are reset.
     * @param process the process to monitor. It cannot be <tt>null</tt>
     * @throws IllegalStateException if a process is still being monitored.
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
        watchdog.start();
    }

    /**
     * Stops the watcher. It will notify all threads possibly waiting
     * on this object.
     */
    public synchronized void stop() {
        watchdog.stop();
        watch = false;
        process = null;
    }

    /**
     * Called after watchdog has finished.
     */
    public void timeoutOccured(Watchdog w) {
        try {
            try {
                // We must check if the process was not stopped
                // before being here
                process.exitValue();
            } catch (IllegalThreadStateException itse) {
                // the process is not terminated, if this is really
                // a timeout and not a manual stop then kill it.
                if (watch) {
                    killedProcess = true;
                    process.destroy();
                }
            }
        } catch (Exception e) {
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
     * This method will rethrow the exception that was possibly caught during
     * the run of the process. It will only remains valid once the process has
     * been terminated either by 'error', timeout or manual intervention.
     * Information will be discarded once a new process is ran.
     * @throws  BuildException  a wrapped exception over the one that was
     * silently swallowed and stored during the process run.
     */
    public void checkException() throws BuildException {
        if (caught != null) {
            throw new BuildException("Exception in ExecuteWatchdog.run: "
                                     + caught.getMessage(), caught);
        }
    }

    /**
     * Indicates whether or not the watchdog is still monitoring the process.
     * @return  <tt>true</tt> if the process is still running, otherwise
     *          <tt>false</tt>.
     */
    public boolean isWatching() {
        return watch;
    }

    /**
     * Indicates whether the last process run was killed on timeout or not.
     * @return  <tt>true</tt> if the process was killed otherwise
     *          <tt>false</tt>.
     */
    public boolean killedProcess() {
        return killedProcess;
    }
}

