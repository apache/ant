/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.LogLevel;

/**
 * This is a task used to log messages in the build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="log"
 */
public class Log
    extends AbstractTask
{
    /**
     * The message to printout when logging
     */
    private String m_message;

    /**
     * The level at which to print out messages.
     */
    private LogLevel m_level = LogLevel.WARN;

    /**
     * Set the level at which the message will be logged.
     *
     * @param the level at which message will be logged
     */
    public void setLevel( final LogLevel level )
    {
        m_level = level;
    }

    /**
     * Set the message to print out when logging message
     */
    public void setMessage( final String message )
    {
        checkNullMessage();
        m_message = message;
    }

    /**
     * Set the message to print out when logging message
     */
    public void addContent( final String message )
    {
        checkNullMessage();
        m_message = message;
    }

    /**
     * Log message at specified level.
     */
    public void execute()
        throws TaskException
    {
        LogLevel.log( getLogger(), m_message, m_level );
    }

    /**
     * Utility message to verify that the message has not already been set.
     */
    private void checkNullMessage()
    {
        if( null != m_message )
        {
            final String message = "Message can only be set once by " +
                "either nested content or the message attribute";
            throw new IllegalStateException( message );
        }
    }
}
