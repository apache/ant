/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

public class ProjectListenerSupport
    implements ProjectListener
{
    protected ProjectListener[]   m_listeners = new ProjectListener[ 0 ];

    public void addProjectListener( final ProjectListener listener )
    {
        final ProjectListener[] listeners = new ProjectListener[ m_listeners.length + 1 ];
        System.arraycopy( m_listeners, 0, listeners, 0, m_listeners.length );
        listeners[ m_listeners.length ] = listener;
        m_listeners = listeners;
    }

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

    public void projectStarted( final String projectName )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].projectStarted( projectName );
        }
    }

    public void projectFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].projectFinished();
        }
    }

    public void targetStarted( String targetName )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].targetStarted( targetName );
        }
    }

    public void targetFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].targetFinished();
        }
    }

    public void taskletStarted( String taskletName )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].taskletStarted( taskletName );
        }
    }

    public void taskletFinished()
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].taskletFinished();
        }
    }

    public void log( String message )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].log( message );
        }
    }

    public void log( String message, Throwable throwable )
    {
        for( int i = 0; i < m_listeners.length; i++ )
        {
            m_listeners[ i ].log( message, throwable );
        }
    }
}
