/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A task to sleep for a period of time
 *
 * @author steve_l@iseran.com steve loughran
 * @created 01 May 2001
 */

public class Sleep extends Task
{
    /**
     * failure flag
     */
    private boolean failOnError = true;

    /**
     * Description of the Field
     */
    private int seconds = 0;
    /**
     * Description of the Field
     */
    private int hours = 0;
    /**
     * Description of the Field
     */
    private int minutes = 0;
    /**
     * Description of the Field
     */
    private int milliseconds = 0;

    /**
     * Creates new instance
     */
    public Sleep()
    {
    }

    /**
     * Sets the FailOnError attribute of the MimeMail object
     *
     * @param failOnError The new FailOnError value
     */
    public void setFailOnError( boolean failOnError )
    {
        this.failOnError = failOnError;
    }

    /**
     * Sets the Hours attribute of the Sleep object
     *
     * @param hours The new Hours value
     */
    public void setHours( int hours )
    {
        this.hours = hours;
    }

    /**
     * Sets the Milliseconds attribute of the Sleep object
     *
     * @param milliseconds The new Milliseconds value
     */
    public void setMilliseconds( int milliseconds )
    {
        this.milliseconds = milliseconds;
    }

    /**
     * Sets the Minutes attribute of the Sleep object
     *
     * @param minutes The new Minutes value
     */
    public void setMinutes( int minutes )
    {
        this.minutes = minutes;
    }

    /**
     * Sets the Seconds attribute of the Sleep object
     *
     * @param seconds The new Seconds value
     */
    public void setSeconds( int seconds )
    {
        this.seconds = seconds;
    }

    /**
     * sleep for a period of time
     *
     * @param millis time to sleep
     */
    public void doSleep( long millis )
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
        try
        {
            validate();
            long sleepTime = getSleepTime();
            log( "sleeping for " + sleepTime + " milliseconds",
                 Project.MSG_VERBOSE );
            doSleep( sleepTime );
        }
        catch( Exception e )
        {
            if( failOnError )
            {
                throw new TaskException( "Error", e );
            }
            else
            {
                String text = e.toString();
                log( text, Project.MSG_ERR );
            }
        }
    }

    /**
     * verify parameters
     *
     * @throws TaskException if something is invalid
     */
    public void validate()
        throws TaskException
    {
        long sleepTime = getSleepTime();
        if( getSleepTime() < 0 )
        {
            throw new TaskException( "Negative sleep periods are not supported" );
        }
    }

    /**
     * return time to sleep
     *
     * @return sleep time. if below 0 then there is an error
     */

    private long getSleepTime()
    {
        return ( ( ( (long)hours * 60 ) + minutes ) * 60 + seconds ) * 1000 + milliseconds;
    }

}

