/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.myrmidon.listeners.LogEvent;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * Support for the project listener event dispatching.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ProjectListenerSupport
    implements LogEvent
{
    private ProjectListener[] m_listeners = new ProjectListener[ 0 ];
    private String m_projectName;
    private String m_targetName;
    private String m_taskName;
    private String m_message;
    private Throwable m_throwable;

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

        if( -1 == found )
        {
            return;
        }

        final ProjectListener[] listeners = new ProjectListener[ m_listeners.length - 1 ];
        System.arraycopy( m_listeners, 0, listeners, 0, found );

        final int count = m_listeners.length - found - 1;
        System.arraycopy( m_listeners, found, listeners, found + 1, count );

        m_listeners = listeners;
    }

    /**
     * Fire a projectStarted event.
     */
    public void projectStarted( final String projectName )
    {
        m_projectName = projectName;
        m_targetName = null;
        m_taskName = null;

        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].projectStarted( this );
        }
    }

    /**
     * Fire a projectFinished event.
     */
    public void projectFinished( final String projectName )
    {
        m_projectName = projectName;

        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].projectFinished( this );
        }

        m_projectName = null;
        m_targetName = null;
        m_taskName = null;
    }

    /**
     * Fire a targetStarted event.
     */
    public void targetStarted( final String projectName, final String targetName )
    {
        m_projectName = projectName;
        m_targetName = targetName;
        m_taskName = null;

        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].targetStarted( this );
        }
    }

    /**
     * Fire a targetFinished event.
     */
    public void targetFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].targetFinished( this );
        }

        m_targetName = null;
        m_taskName = null;
    }

    /**
     * Fire a targetStarted event.
     */
    public void taskStarted( final String taskName )
    {
        m_taskName = taskName;

        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].taskStarted( this );
        }
    }

    /**
     * Fire a taskFinished event.
     */
    public void taskFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].taskFinished( this );
        }

        m_taskName = null;
    }

    /**
     * Fire a log event.
     *
     * @param message the log message
     */
    public void log( String message, Throwable throwable )
    {
        m_message = message;
        m_throwable = throwable;

        try
        {
            for( int i = 0; i < m_listeners.length; i++ )
            {
                m_listeners[ i ].log( this );
            }
        }
        finally
        {
            m_message = null;
            m_throwable = null;
        }
    }

    /**
     * Returns the message.
     */
    public String getMessage()
    {
        return m_message;
    }

    /**
     * Returns the error that occurred.
     */
    public Throwable getThrowable()
    {
        return m_throwable;
    }

    /**
     * Returns the name of the task.
     */
    public String getTaskName()
    {
        return m_taskName;
    }

    /**
     * Returns the name of the target.
     */
    public String getTargetName()
    {
        return m_targetName;
    }

    /**
     * Returns the name of the project.
     */
    public String getProjectName()
    {
        return m_projectName;
    }
}
