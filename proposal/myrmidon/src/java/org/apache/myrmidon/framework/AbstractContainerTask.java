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
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.converter.ConverterException;
import org.apache.myrmidon.components.configurer.Configurer;
import org.apache.myrmidon.components.converter.MasterConverter;
import org.apache.myrmidon.components.executor.Executor;

/**
 * This is the class that Task writers should extend to provide custom tasks.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractContainerTask
    extends AbstractTask
    implements Composable
{
    ///For converting own attributes
    private MasterConverter     m_converter;

    ///For configuring own sub-elements
    private Configurer          m_configurer;

    ///For executing sub-elements as tasks
    private Executor            m_executor;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_configurer = (Configurer)componentManager.lookup( Configurer.ROLE );
        m_converter = (MasterConverter)componentManager.lookup( MasterConverter.ROLE );
        m_executor = (Executor)componentManager.lookup( Executor.ROLE );
    }

    /**
     * Helper method to resolve a string.
     *
     * @param value the value to resolve
     * @return the resolved value
     * @exception ConfigurationException if an error occurs
     */
    protected final Object resolve( final String value )
        throws ConfigurationException
    {
        try
        {
            final Object object = getContext().resolveValue( value );

            if( null == object )
            {
                throw new ConfigurationException( "Value (" + value +
                                                  ") resolved to null" );
            }

            return object;
        }
        catch( final TaskException te )
        {
            throw new ConfigurationException( "Error resolving value: " + value, te );
        }
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
            throw new ConfigurationException( "Error converting value", ce );
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
