/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.net;

import com.oroinc.net.telnet.TelnetClient;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * This class handles the abstraction of the telnet protocol. Currently it
 * is a wrapper around <a href="www.oroinc.com">ORO</a> 's NetComponents
 */
public class AntTelnetClient
    extends TelnetClient
{
    private TelnetTask m_task;

    public AntTelnetClient( final TelnetTask task )
    {
        m_task = task;
    }

    /**
     * Write this string to the telnet session.
     */
    public void sendString( final String string, final boolean echoString )
        throws TaskException
    {
        final OutputStream output = this.getOutputStream();
        m_task.doSendString( output, string, echoString );
    }

    /**
     * Read from the telnet session until the string we are waiting for is
     * found
     */
    public void waitForString( final String string )
        throws TaskException
    {
        waitForString( string, null );
    }

    /**
     * Read from the telnet session until the string we are waiting for is
     * found or the timeout has been reached
     *
     * @parm s The string to wait on
     * @parm timeout The maximum number of seconds to wait
     */
    public void waitForString( final String string,
                               final Integer timeout )
        throws TaskException
    {
        final InputStream input = this.getInputStream();
        m_task.doWaitForString( input, string, timeout );
    }
}
