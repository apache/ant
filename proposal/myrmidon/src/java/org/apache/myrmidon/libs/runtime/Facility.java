/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.runtime;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.aspects.AspectHandler;
import org.apache.myrmidon.interfaces.aspect.AspectManager;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.framework.AbstractContainerTask;

/**
 * Task that definesMethod to register a single converter.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Facility
    extends AbstractContainerTask
    implements Composable, Configurable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Facility.class );

    private String              m_namespace;
    private AspectHandler       m_aspectHandler;

    private AspectManager       m_aspectManager;
    private TypeFactory         m_factory;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        super.compose( componentManager );

        m_aspectManager = (AspectManager)componentManager.lookup( AspectManager.ROLE );

        final TypeManager typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        try { m_factory = typeManager.getFactory( AspectHandler.ROLE ); }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "facility.no-factory.error" );
            throw new ComponentException( message, te );
        }
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        final String[] attributes = configuration.getAttributeNames();
        for( int i = 0; i < attributes.length; i++ )
        {
            final String name = attributes[ i ];
            final String value = configuration.getAttribute( name );
            configure( this, name, value );
        }

        final Configuration[] children = configuration.getChildren();

        if( 1 == children.length )
        {
            try
            {
                m_aspectHandler = (AspectHandler)m_factory.create( children[ 0 ].getName() );
            }
            catch( final Exception e )
            {
                final String message = 
                    REZ.getString( "facility.no-create.error", children[ 0 ].getName() );
                throw new ConfigurationException( message, e );
            }

            configure( m_aspectHandler, children[ 0 ] );
        }
        else
        {
            final String message = REZ.getString( "facility.multi-element.error" );
            throw new ConfigurationException( message );
        }
    }

    public void setNamespace( final String namespace )
    {
        m_namespace = namespace;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_namespace )
        {
            final String message = REZ.getString( "facility.no-namespace.error" );
            throw new TaskException( message );
        }

        m_aspectManager.addAspectHandler( m_namespace, m_aspectHandler );
    }
}
