/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.type.TypeFactory;

/**
 * This is the property "task" to declare a binding of a datatype to a name.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class TypeInstanceTask
    extends AbstractContainerTask
    implements Configurable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( TypeInstanceTask.class );

    private String m_id;
    private Object m_value;
    private boolean m_localScope = true;

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
            final TypeFactory typeFactory = getTypeFactory( DataType.class );
            m_value = typeFactory.create( configuration.getName() );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "type.no-create.error" );
            throw new ConfigurationException( message, e );
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
            final String message = REZ.getString( "type.no-id.error" );
            throw new TaskException( message );
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
