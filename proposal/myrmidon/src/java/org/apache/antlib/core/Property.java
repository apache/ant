/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.AbstractContainerTask;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.type.TypeFactory;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 *
 * TODO: Determine final format of property task.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:task name="property"
 */
public class Property
    extends AbstractContainerTask
    implements Configurable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Property.class );

    private String m_name;
    private Object m_value;
    private boolean m_localScope = true;

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
        for( int i = 0; i < children.length; i++ )
        {
            try
            {
                final String typeName = children[ i ].getName();
                final DataType value = (DataType)newInstance( DataType.class, typeName );
                configure( value, children[ i ] );
                setValue( value );
            }
            catch( final Exception e )
            {
                final String message = REZ.getString( "property.no-set.error" );
                throw new ConfigurationException( message, e );
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
            final String message = REZ.getString( "property.multi-set.error" );
            throw new TaskException( message );
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
            final String message = REZ.getString( "property.no-name.error" );
            throw new TaskException( message );
        }

        if( null == m_value )
        {
            final String message = REZ.getString( "property.no-value.error" );
            throw new TaskException( message );
        }

        if( m_localScope )
        {
            getContext().setProperty( m_name, m_value );
        }
        else
        {
            getContext().setProperty( m_name, m_value, TaskContext.PARENT );
        }
    }
}
