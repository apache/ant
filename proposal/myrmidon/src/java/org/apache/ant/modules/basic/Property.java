/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Resolvable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.DataType;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.configurer.Configurer;
import org.apache.myrmidon.components.converter.MasterConverter;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.components.type.TypeException;
import org.apache.myrmidon.components.type.TypeFactory;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Property
    extends AbstractTask
    implements Configurable, Composable
{
    private String              m_name;
    private Object              m_value;
    private boolean             m_localScope     = true;
    private TypeFactory         m_factory;
    private MasterConverter     m_converter;
    private Configurer          m_configurer;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_configurer = (Configurer)componentManager.lookup( Configurer.ROLE );
        final TypeManager typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        
        try { m_factory = typeManager.getFactory( DataType.ROLE ); }
        catch( final TypeException te )
        {
            throw new ComponentException( "Unable to retrieve factory from TypeManager", te );
        }

        m_converter = (MasterConverter)componentManager.lookup( MasterConverter.ROLE );
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        final String[] attributes = configuration.getAttributeNames();

        for( int i = 0; i < attributes.length; i++ )
        {
            final String name = attributes[ i ];
            final String value = configuration.getAttribute( name );


            Object object = null;

            try { object = getContext().resolveValue( value ); }
            catch( final TaskException te )
            {
                throw new ConfigurationException( "Error resolving value: " + value, te );
            }

            if( null == object )
            {
                throw new ConfigurationException( "Value for attribute " + name + "resolved to null" );
            }

            if( name.equals( "name" ) )
            {
                try
                {
                    final String convertedValue =
                        (String)m_converter.convert( String.class, object, getContext() );
                    setName( convertedValue );
                }
                catch( final Exception e )
                {
                    throw new ConfigurationException( "Error converting value", e );
                }
            }
            else if( name.equals( "value" ) )
            {
                try { setValue( object ); }
                catch( final TaskException te )
                {
                    throw new ConfigurationException( "Error setting value: " + value, te );
                }
            }
            else if( name.equals( "local-scope" ) )
            {
                try
                {
                    final Boolean localScope =
                        (Boolean)m_converter.convert( Boolean.class, object, getContext() );
                    setLocalScope( Boolean.TRUE == localScope );
                }
                catch( final Exception e )
                {
                    throw new ConfigurationException( "Error converting value", e );
                }
            }
            else
            {
                throw new ConfigurationException( "Unknown attribute " + name );
            }
        }

        final Configuration[] children = configuration.getChildren();

        for( int i = 0; i < children.length; i++ )
        {
            final Configuration child = children[ i ];

            try
            {
                final DataType value = (DataType)m_factory.create( child.getName() );
                setValue( value );
                m_configurer.configure( value, child, getContext() );
            }
            catch( final Exception e )
            {
                throw new ConfigurationException( "Unable to set datatype", e );
            }
        }
    }

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setValue( final Object value )
        throws TaskException
    {
        if( null != m_value )
        {
            throw new TaskException( "Value can not be set multiple times" );
        }

        m_value = value;
    }

    public void setLocalScope( final boolean localScope )
    {
        m_localScope = localScope;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_name )
        {
            throw new TaskException( "Name must be specified" );
        }

        if( null == m_value )
        {
            throw new TaskException( "Value must be specified" );
        }

        final TaskContext context = getContext();

        Object value = m_value;

        if( value instanceof String )
        {
            value = context.resolveValue( (String)value );
        }

        while( null != value && value instanceof Resolvable )
        {
            value = ((Resolvable)value).resolve( context );
        }

        if( m_localScope )
        {
            context.setProperty( m_name, value );
        }
        else
        {
            context.setProperty( m_name, value, TaskContext.PARENT );
        }
    }
}
