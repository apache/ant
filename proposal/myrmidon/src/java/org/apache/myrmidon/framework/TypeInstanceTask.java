/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.myrmidon.api.DataType;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.type.TypeException;
import org.apache.myrmidon.components.type.TypeFactory;
import org.apache.myrmidon.components.type.TypeManager;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class TypeInstanceTask
    extends AbstractContainerTask
    implements Configurable
{
    private String              m_id;
    private Object              m_value;
    private boolean             m_localScope     = true;
    private TypeFactory         m_factory;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        super.compose( componentManager );

        final TypeManager typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        try { m_factory = typeManager.getFactory( DataType.ROLE ); }
        catch( final TypeException te )
        {
            throw new ComponentException( "Unable to retrieve factory from TypeManager", te );
        }
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        final DefaultConfiguration newConfiguration =
            new DefaultConfiguration( configuration.getName(), configuration.getLocation() );

        final String[] attributes = configuration.getAttributeNames();
        for( int i = 0; i < attributes.length; i++ )
        {
            final String name = attributes[ i ];
            final String value = configuration.getAttribute( name );

            if( name.equals( "id" ) || name.equals( "local-scope" ) )
            {
                configure( this, name, value );
            }
            else
            {
                newConfiguration.setAttribute( name, value );
            }
        }

        final Configuration[] children = configuration.getChildren();
        for( int i = 0; i < children.length; i++ )
        {
            newConfiguration.addChild( children[ i ] );
        }

        try
        {
            m_value = m_factory.create( configuration.getName() );
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( "Unable to create datatype", e );
        }

        configure( m_value, newConfiguration );
    }

    public void setId( final String id )
    {
        m_id = id;
    }

    public void setLocalScope( final boolean localScope )
    {
        m_localScope = localScope;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_id )
        {
            throw new TaskException( "Id must be specified" );
        }

        if( m_localScope )
        {
            getContext().setProperty( m_id, m_value );
        }
        else
        {
            getContext().setProperty( m_id, m_value, TaskContext.PARENT );
        }
    }
}
