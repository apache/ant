/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

import org.apache.avalon.framework.ExceptionUtil;

/**
 * Default listener that emulates the old ant listener notifications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultProjectListener
    extends AbstractProjectListener
{
    private String m_prefix;
    private String m_targetName;

    /**
     * Notify listener of targetStarted event.
     *
     * @param targetName the name of target
     */
    public void targetStarted( final String targetName )
    {
        m_targetName = targetName;
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
        if( null != m_targetName )
        {
            System.out.println( m_targetName + ":\n" );
            m_targetName = null;
        }

        if( null != getPrefix() )
            System.out.println( "\t[" + getPrefix() + "] " + data );
        else
            System.out.println( data );
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
