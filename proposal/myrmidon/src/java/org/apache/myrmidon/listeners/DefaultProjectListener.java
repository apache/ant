/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.listeners;

import org.apache.avalon.framework.ExceptionUtil;

/**
 * Default listener that emulates the old ant listener notifications.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectListener
    implements ProjectListener
{
    private String        m_prefix;

    /**
     * Notify listener of projectStarted event.
     */
    public void projectStarted()
    {
    }

    /**
     * Notify listener of projectFinished event.
     */
    public void projectFinished()
    {
    }

    /**
     * Notify listener of targetStarted event.
     *
     * @param targetName the name of target
     */
    public void targetStarted( final String targetName )
    {
        output( targetName + ":\n" );
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished()
    {
    }

    /**
     * Notify listener of taskStarted event.
     *
     * @param taskName the name of task
     */
    public void taskStarted( final String taskName )
    {
        setPrefix( taskName );
    }

    /**
     * Notify listener of taskFinished event.
     */
    public void taskFinished()
    {
        setPrefix( null );
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     */
    public void log( String message )
    {
        output( message );
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void log( String message, Throwable throwable )
    {
        output( message + "\n" + ExceptionUtil.printStackTrace( throwable, 5, true ) );
    }

    /**
     * Utility class to output data.
     * Overide in sub-classes to direct to a different destination.
     *
     * @param data the data
     */
    protected void output( final String data )
    {
        if( null != getPrefix() ) System.out.println( "\t[" + getPrefix() + "] " + data );
        else System.out.println( data );
    }

    protected final String getPrefix()
    {
        return m_prefix;
    }

    protected final void setPrefix( final String prefix )
    {
        m_prefix = prefix;
    }
}
