/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.exec;

import org.apache.myrmidon.api.TaskException;

/**
 * Destroys a process running for too long. For example: <pre>
 * ExecuteWatchdog watchdog = new ExecuteWatchdog(30000);
 * Execute exec = new Execute(myloghandler, watchdog);
 * exec.setCommandLine(mycmdline);
 * int exitvalue = exec.execute();
 * if (exitvalue != SUCCESS && watchdog.killedProcess()){
 *              // it was killed on purpose by the watchdog
 * }
 * </pre>
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @see Execute
 */
public class ExecuteWatchdog
    implements Runnable
{
    /**
     * say whether or not the watchog is currently monitoring a process
     */
    private boolean m_watch;

    /**
     * exception that might be thrown during the process execution
     */
    private Exception m_caught;

    /**
     * say whether or not the process was killed due to running overtime
     */
    private boolean m_killedProcess;

    /**
     * the process to execute and watch for duration
     */
    private Process m_process;

    /**
     * timeout duration. Once the process running time exceeds this it should be
     * killed
     */
    private int m_timeout;

    /**
     * Creates a new watchdog with a given timeout.
     *
     * @param timeout the timeout for the process in milliseconds. It must be
     *      greather than 0.
     */
    public ExecuteWatchdog( int timeout )
    {
        if( timeout < 1 )
        {
            throw new IllegalArgumentException( "timeout lesser than 1." );
        }
        this.m_timeout = timeout;
    }

    /**
     * Indicates whether or not the watchdog is still monitoring the process.
     *
     * @return <tt>true</tt> if the process is still running, otherwise <tt>
     *      false</tt> .
     */
    public boolean isWatching()
    {
        return m_watch;
    }

    /**
     * This method will rethrow the exception that was possibly caught during
     * the run of the process. It will only remains valid once the process has
     * been terminated either by 'error', timeout or manual intervention.
     * Information will be discarded once a new process is ran.
     *
     * @throws TaskException a wrapped exception over the one that was silently
     *      swallowed and stored during the process run.
     */
    public void checkException()
        throws TaskException
    {
        if( m_caught != null )
        {
            throw new TaskException( "Exception in ExecuteWatchdog.run: "
                                     + m_caught.getMessage(), m_caught );
        }
    }

    /**
     * Indicates whether the last process run was killed on timeout or not.
     *
     * @return <tt>true</tt> if the process was killed otherwise <tt>false</tt>
     *      .
     */
    public boolean killedProcess()
    {
        return m_killedProcess;
    }

    /**
     * Watches the process and terminates it, if it runs for to long.
     */
    public synchronized void run()
    {
        try
        {
            // This isn't a Task, don't have a Project object to log.
            // project.log("ExecuteWatchdog: timeout = "+timeout+" msec",  Project.MSG_VERBOSE);
            final long until = System.currentTimeMillis() + m_timeout;
            long now;
            while( m_watch && until > ( now = System.currentTimeMillis() ) )
            {
                try
                {
                    wait( until - now );
                }
                catch( InterruptedException e )
                {
                }
            }

            // if we are here, either someone stopped the watchdog,
            // we are on timeout and the process must be killed, or
            // we are on timeout and the process has already stopped.
            try
            {
                // We must check if the process was not stopped
                // before being here
                m_process.exitValue();
            }
            catch( IllegalThreadStateException e )
            {
                // the process is not terminated, if this is really
                // a timeout and not a manual stop then kill it.
                if( m_watch )
                {
                    m_killedProcess = true;
                    m_process.destroy();
                }
            }
        }
        catch( Exception e )
        {
            m_caught = e;
        }
        finally
        {
            cleanUp();
        }
    }

    /**
     * Watches the given process and terminates it, if it runs for too long. All
     * information from the previous run are reset.
     *
     * @param process the process to monitor. It cannot be <tt>null</tt>
     * @throws IllegalStateException thrown if a process is still being
     *      monitored.
     */
    public synchronized void start( Process process )
    {
        if( process == null )
        {
            throw new NullPointerException( "process is null." );
        }
        if( this.m_process != null )
        {
            throw new IllegalStateException( "Already running." );
        }
        this.m_caught = null;
        this.m_killedProcess = false;
        this.m_watch = true;
        this.m_process = process;
        final Thread thread = new Thread( this, "WATCHDOG" );
        thread.setDaemon( true );
        thread.start();
    }

    /**
     * Stops the watcher. It will notify all threads possibly waiting on this
     * object.
     */
    public synchronized void stop()
    {
        m_watch = false;
        notifyAll();
    }

    /**
     * reset the monitor flag and the process.
     */
    protected void cleanUp()
    {
        m_watch = false;
        m_process = null;
    }
}

