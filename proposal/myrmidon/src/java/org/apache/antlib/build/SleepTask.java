/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.build;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * A task to sleep for a period of time
 *
 * @ant:task name="sleep"
 * @author steve_l@iseran.com steve loughran
 * @version $Revision$ $Date$
 */
public class SleepTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( SleepTask.class );

    private int m_seconds;
    private int m_hours;
    private int m_minutes;
    private int m_milliseconds;

    /**
     * Sets the Hours attribute of the Sleep object
     */
    public void setHours( final int hours )
    {
        m_hours = hours;
    }

    /**
     * Sets the Milliseconds attribute of the Sleep object
     */
    public void setMilliseconds( final int milliseconds )
    {
        m_milliseconds = milliseconds;
    }

    /**
     * Sets the Minutes attribute of the Sleep object
     */
    public void setMinutes( final int minutes )
    {
        m_minutes = minutes;
    }

    /**
     * Sets the Seconds attribute of the Sleep object
     */
    public void setSeconds( final int seconds )
    {
        m_seconds = seconds;
    }

    /**
     * sleep for a period of time
     *
     * @param millis time to sleep
     */
    private void doSleep( final long millis )
    {
        try
        {
            Thread.currentThread().sleep( millis );
        }
        catch( InterruptedException ie )
        {
        }
    }

    /**
     * Executes this build task. throws org.apache.tools.ant.TaskException if
     * there is an error during task execution.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        validate();
        final long sleepTime = getSleepTime();

        final String message = REZ.getString( "sleep.duration.notice", new Long( sleepTime ) );
        getLogger().debug( message );

        doSleep( sleepTime );
    }

    /**
     * verify parameters
     *
     * @throws TaskException if something is invalid
     */
    private void validate()
        throws TaskException
    {
        if( getSleepTime() < 0 )
        {
            final String message = REZ.getString( "sleep.neg-time.error" );
            throw new TaskException( message );
        }
    }

    /**
     * return time to sleep
     *
     * @return sleep time. if below 0 then there is an error
     */
    private long getSleepTime()
    {
        return ( ( ( (long)m_hours * 60 ) + m_minutes ) * 60 + m_seconds ) * 1000 + m_milliseconds;
    }
}

