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
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.converter.ConverterException;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * This is the class that Task writers should extend to provide custom tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractContainerTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( AbstractContainerTask.class );

    ///For converting own attributes
    private MasterConverter m_converter;

    ///For configuring own sub-elements
    private Configurer m_configurer;

    ///For executing sub-elements as tasks
    private Executor m_executor;

    /**
     * Retrieve context from container.
     *
     * @param context the context
     */
    public void contextualize( TaskContext context )
        throws TaskException
    {
        super.contextualize( context );
        m_configurer = (Configurer)getService( Configurer.class );
        m_converter = (MasterConverter)getService( MasterConverter.class );
        m_executor = (Executor)getService( Executor.class );
    }

    /**
     * Helper method to convert an object to a specific type.
     *
     * @param to type to convert object to
     * @param object the object to convert
     * @return the converted object
     * @exception ConfigurationException if an error occurs
     */
    protected final Object convert( final Class to, final Object object )
        throws ConfigurationException
    {
        try
        {
            return getConverter().convert( to, object, getContext() );
        }
        catch( final ConverterException ce )
        {
            final String message = REZ.getString( "container.bad-config.error" );
            throw new ConfigurationException( message, ce );
        }
    }

    /**
     * Configure an object using specific configuration element.
     *
     * @param object the object
     * @param element the configuration element
     * @exception ConfigurationException if an error occurs
     */
    protected final void configure( final Object object, final Configuration element )
        throws ConfigurationException
    {
        getConfigurer().configure( object, element, getContext() );
    }

    /**
     * Configure an objects attribute using parameters.
     *
     * @param object the object
     * @param name the attibute name
     * @param value the attibute value
     * @exception ConfigurationException if an error occurs
     */
    protected final void configure( final Object object, final String name, final String value )
        throws ConfigurationException
    {
        getConfigurer().configure( object, name, value, getContext() );
    }

    /**
     * Create an instance of type with specified type and in specified role.
     */
    protected final Object newInstance( final Class roleType, final String typeName )
        throws TaskException
    {
        final TypeFactory typeFactory = getTypeFactory( roleType );
        try
        {
            return typeFactory.create( typeName );
        }
        catch( final TypeException te )
        {
            final String message =
                REZ.getString( "container.no-create-type.error",
                               roleType.getName(),
                               typeName );
            throw new TaskException( message, te );
        }
    }

    /**
     * Locates a type factory.
     */
    protected final TypeFactory getTypeFactory( final Class roleType )
        throws TaskException
    {
        final TypeManager typeManager = (TypeManager)getService( TypeManager.class );
        try
        {
            return typeManager.getFactory( roleType );
        }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "container.no-factory.error", roleType.getName() );
            throw new TaskException( message, te );
        }
    }

    /**
     * Convenience method for sub-class to retrieve Configurer.
     *
     * @return the configurer
     */
    protected final Configurer getConfigurer()
    {
        return m_configurer;
    }

    /**
     * Convenience method for sub-class to retrieve Converter.
     *
     * @return the converter
     */
    protected final Converter getConverter()
    {
        return m_converter;
    }

    /**
     * Convenience method for sub-class to retrieve Executor.
     *
     * @return the executor
     */
    protected final Executor getExecutor()
    {
        return m_executor;
    }
}
