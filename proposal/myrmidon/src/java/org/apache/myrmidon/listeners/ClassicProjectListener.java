/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

import java.io.PrintWriter;
import org.apache.avalon.framework.ExceptionUtil;

/**
 * Classic listener that emulates the default ant1.x listener.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ClassicProjectListener
    extends AbstractProjectListener
{
    private final PrintWriter m_printWriter;

    public ClassicProjectListener()
    {
        m_printWriter = new PrintWriter( System.out, true );
    }

    /**
     * Notify listener of targetStarted event.
     */
    public void targetStarted( final TargetEvent event )
    {
        writeTargetHeader( event );
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished( TargetEvent event )
    {
        getWriter().println();
    }

    /**
     * Notify listener of log message event.
     */
    public void log( final LogEvent event )
    {
        writeMessage( event );
        writeThrowable( event );
    }

    /**
     * Returns the PrintWriter to write to.
     */
    protected PrintWriter getWriter()
    {
        return m_printWriter;
    }

    /**
     * Writes the target header.
     */
    protected void writeTargetHeader( final TargetEvent event )
    {
        getWriter().println( event.getTargetName() + ":" );
    }

    /**
     * Writes a message
     */
    protected void writeMessage( final LogEvent event )
    {
        // Write the message
        final String message = event.getMessage();
        final String task = event.getTaskName();
        if( null != task )
        {
            getWriter().println( "\t[" + task + "] " + message );
        }
        else
        {
            getWriter().println( message );
        }
    }

    /**
     * Writes a throwable.
     */
    private void writeThrowable( final LogEvent event )
    {
        // Write the exception, if any
        final Throwable throwable = event.getThrowable();
        if( throwable != null )
        {
            getWriter().println( ExceptionUtil.printStackTrace( throwable, 5, true ) );
        }
    }
}
