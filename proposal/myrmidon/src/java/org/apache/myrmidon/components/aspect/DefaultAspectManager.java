/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.aspect;

import java.util.ArrayList;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Logger;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.aspects.AspectHandler;

/**
 * Manage and propogate Aspects.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultAspectManager
    implements AspectManager
{
    private ArrayList            m_aspectCopy  = new ArrayList();
    private AspectHandler[]      m_aspects     = new AspectHandler[ 0 ];

    public synchronized void addAspectHandler( final AspectHandler handler )
    {
        m_aspectCopy.add( handler );
        m_aspects = (AspectHandler[])m_aspectCopy.toArray( m_aspects );
    }

    public synchronized void removeAspectHandler( final AspectHandler handler )
    {
        m_aspectCopy.remove( handler );
        m_aspects = (AspectHandler[])m_aspectCopy.toArray( m_aspects );
    }

    public Configuration preCreate( final Configuration configuration )
        throws TaskException
    {
        Configuration model = configuration;

        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            model = aspects[ i ].preCreate( model );
        }

        return model;
    }

    public void aspect( final Parameters parameters, final Configuration[] elements )
        throws TaskException
    {
        throw new UnsupportedOperationException( "Can not provide parameters to AspectManager" ); 
    }

    public void postCreate( final Task task )
        throws TaskException
    {
        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            aspects[ i ].postCreate( task );
        }
    }

    public void preLoggable( final Logger logger )
        throws TaskException
    {
        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            aspects[ i ].preLoggable( logger );
        }
    }

    public void preConfigure( final Configuration taskModel )
        throws TaskException
    {
        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            aspects[ i ].preConfigure( taskModel );
        }
    }

    public void preExecute()
        throws TaskException
    {
        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            aspects[ i ].preExecute();
        }
    }

    public void preDestroy()
        throws TaskException
    {
        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            aspects[ i ].preDestroy();
        }
    }

    public boolean error( final TaskException te )
        throws TaskException
    {
        final AspectHandler[] aspects = m_aspects;
        for( int i = 0; i < aspects.length; i++ )
        {
            if( true == aspects[ i ].error( te ) )
            {
                return true;
            }
        }

        return false;
    }
}
