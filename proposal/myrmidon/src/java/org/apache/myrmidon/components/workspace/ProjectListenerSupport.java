/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.myrmidon.listeners.ProjectListener;

/**
 * Support for the project listener event dispatching.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ProjectListenerSupport
    implements ProjectListener
{
    private ProjectListener[] m_listeners = new ProjectListener[ 0 ];

    /**
     * Add an extra project listener that wants to receive notification of listener events.
     *
     * @param listener the listener
     */
    public void addProjectListener( final ProjectListener listener )
    {
        final ProjectListener[] listeners = new ProjectListener[ m_listeners.length + 1 ];
        System.arraycopy( m_listeners, 0, listeners, 0, m_listeners.length );
        listeners[ m_listeners.length ] = listener;
        m_listeners = listeners;
    }

    /**
     * Remove a project listener that wants to receive notification of listener events.
     *
     * @param listener the listener
     */
    public void removeProjectListener( final ProjectListener listener )
    {
        int found = -1;

        for( int i = 0; i < m_listeners.length; i++ )
        {
            if( listener == m_listeners[ i ] )
            {
                found = i;
                break;
            }
        }

        if( -1 == found ) return;

        final ProjectListener[] listeners = new ProjectListener[ m_listeners.length - 1 ];
        System.arraycopy( m_listeners, 0, listeners, 0, found );

        final int count = m_listeners.length - found - 1;
        System.arraycopy( m_listeners, found, listeners, found + 1, count );

        m_listeners = listeners;
    }

    /**
     * Fire a projectStarted event.
     */
    public void projectStarted()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].projectStarted();
        }
    }

    /**
     * Fire a projectFinished event.
     */
    public void projectFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].projectFinished();
        }
    }

    /**
     * Fire a targetStarted event.
     *
     * @param targetName the name of target
     */
    public void targetStarted( String targetName )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].targetStarted( targetName );
        }
    }

    /**
     * Fire a targetFinished event.
     */
    public void targetFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].targetFinished();
        }
    }

    /**
     * Fire a targetStarted event.
     *
     * @param targetName the name of target
     */
    public void taskStarted( String taskName )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].taskStarted( taskName );
        }
    }

    /**
     * Fire a taskFinished event.
     */
    public void taskFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].taskFinished();
        }
    }

    /**
     * Fire a log event.
     *
     * @param message the log message
     */
    public void log( String message )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].log( message );
        }
    }

    /**
     * Fire a log event.
     *
     * @param message the log message
     * @param throwable the throwable to be logged
     */
    public void log( String message, Throwable throwable )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].log( message, throwable );
        }
    }
}
