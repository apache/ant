/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.net;

import org.apache.myrmidon.api.TaskException;

/**
 * This class reads the output from the connected server until the required
 * string is found.
 */
public class TelnetRead
    extends TelnetSubTask
{
    private Integer m_timeout;

    /**
     * Sets the default timeout if none has been set already
     */
    public void setDefaultTimeout( final Integer defaultTimeout )
    {
        if( m_timeout == null )
        {
            m_timeout = defaultTimeout;
        }
    }

    /**
     * Override any default timeouts
     */
    public void setTimeout( final Integer timeout )
    {
        m_timeout = timeout;
    }

    public void execute( final AntTelnetClient telnet )
        throws TaskException
    {
        telnet.waitForString( getTaskString(), m_timeout );
    }
}
