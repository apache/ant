/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.aspect;

import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Logger;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.aspects.AspectHandler;
import org.apache.myrmidon.aspects.NoopAspectHandler;
import org.apache.myrmidon.interfaces.aspect.AspectManager;

/**
 * Manage and propogate Aspects.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultAspectManager
    implements AspectManager, Initializable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultAspectManager.class );

    private HashMap          m_aspectMap  = new HashMap();
    private AspectHandler[]  m_aspects    = new AspectHandler[ 0 ];
    private String[]         m_names      = new String[ 0 ];

    public void initialize()
        throws Exception
    {
        ///UGLY HACK!!!!
        addAspectHandler( "ant", new NoopAspectHandler() );
        addAspectHandler( "doc", new NoopAspectHandler() );
    }

    public synchronized void addAspectHandler( final String name, final AspectHandler handler )
        throws TaskException
    {
        m_aspectMap.put( name, handler );
        rebuildArrays();
    }

    public synchronized void removeAspectHandler( final String name, final AspectHandler handler )
        throws TaskException
    {
        final AspectHandler entry = (AspectHandler)m_aspectMap.remove( name );
        if( null == entry )
        {
            final String message = REZ.getString( "no.aspect", name );
            throw new TaskException( message );
        }

        rebuildArrays();
    }

    private void rebuildArrays()
    {
        m_aspects = (AspectHandler[])m_aspectMap.values().toArray( m_aspects );
        m_names = (String[])m_aspectMap.keySet().toArray( m_names );
    }

    public String[] getNames()
    {
        return m_names;
    }

    public void dispatchAspectSettings( final String name,
                                        final Parameters parameters,
                                        final Configuration[] elements )
        throws TaskException
    {
        final AspectHandler handler = (AspectHandler)m_aspectMap.get( name );
        if( null == handler )
        {
            final String message = REZ.getString( "no.aspect", name );
            throw new TaskException( message );
        }

        handler.aspectSettings( parameters, elements );
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

    public void aspectSettings( final Parameters parameters, final Configuration[] elements )
        throws TaskException
    {
        final String message = REZ.getString( "no.settings" );
        throw new UnsupportedOperationException( message );
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
