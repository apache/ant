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

/**
 * This is a task used to log messages in the build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Log
    extends AbstractTask
{
    private String      m_message;


    public void setMessage( final String message )
    {
        checkNullMessage();
        m_message = message;
    }

    public void addContent( final String message )
    {
        checkNullMessage();
        m_message = message;
    }

    public void execute()
        throws TaskException
    {
        getLogger().warn( m_message );
    }

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
