/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.aut.converter.Converter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.role.RoleManager;

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
    private Converter m_converter;

    ///For configuring own sub-elements
    private Configurer m_configurer;

    ///For executing sub-elements as tasks
    private Executor m_executor;
    private ExecutionFrame m_frame;

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
        m_converter = (Converter)getService( Converter.class );
        m_executor = (Executor)getService( Executor.class );
        m_frame = (ExecutionFrame)getService( ExecutionFrame.class );
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
            return m_converter.convert( to, object, getContext() );
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
    protected final void configureElement( final Object object,
                                           final Configuration element )
        throws ConfigurationException
    {
        m_configurer.configureElement( object, element, getContext() );
    }

    /**
     * Configure an object using specific configuration element.
     *
     * @param object the object
     * @param clazz the class to use when configuring element
     * @param element the configuration element
     * @exception ConfigurationException if an error occurs
     */
    protected final void configureElement( final Object object,
                                           final Class clazz,
                                           final Configuration element )
        throws ConfigurationException
    {
        m_configurer.configureElement( object, clazz, element, getContext() );
    }

    /**
     * Configure an objects attribute using parameters.
     *
     * @param object the object
     * @param name the attibute name
     * @param value the attibute value
     * @exception ConfigurationException if an error occurs
     */
    protected final void configureAttribute( final Object object, final String name, final String value )
        throws ConfigurationException
    {
        m_configurer.configureAttribute( object, name, value, getContext() );
    }

    /**
     * Configure an objects attribute using parameters.
     *
     * @param object the object
     * @param clazz the class to use when configuring element
     * @param name the attibute name
     * @param value the attibute value
     * @exception ConfigurationException if an error occurs
     */
    protected final void configureAttribute( final Object object,
                                             final Class clazz,
                                             final String name, final String value )
        throws ConfigurationException
    {
        m_configurer.configureAttribute( object, clazz, name, value, getContext() );
    }

    /**
     * Utility method to execute specified tasks in current ExecutionFrame.
     */
    protected final void executeTasks( final Configuration[] tasks )
        throws TaskException
    {
        for( int i = 0; i < tasks.length; i++ )
        {
            final Configuration task = tasks[ i ];
            executeTask( task );
        }
    }

    /**
     * Utility method to execute specified task in current ExecutionFrame.
     */
    protected final void executeTask( final Configuration task )
        throws TaskException
    {
        m_executor.execute( task, m_frame );
    }

    /**
     * Create an instance of type with specified type and in specified role.
     */
    protected final Object newInstance( final Class roleType, final String typeName )
        throws TaskException
    {
        try
        {
            final RoleInfo role = getRoleByType( roleType );
            final TypeFactory typeFactory = getTypeFactory( role.getName() );
            return typeFactory.create( typeName );
        }
        catch( Exception e )
        {
            final String message =
                REZ.getString( "container.no-create-type-for-type.error", roleType.getName(), typeName );
            throw new TaskException( message, e );
        }
    }

    /**
     * Create an instance of type with specified type and in specified role.
     */
    protected final Object newInstance( final String roleName, final String typeName )
        throws TaskException
    {
        try
        {
            final TypeFactory typeFactory = getTypeFactory( roleName );
            return typeFactory.create( typeName );
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "container.no-create-type.error", roleName, typeName );
            throw new TaskException( message, e );
        }
    }

    /**
     * Looks up a role using the role type.
     */
    protected final RoleInfo getRoleByType( final Class roleType )
        throws TaskException
    {
        final RoleManager roleManager = (RoleManager)getService( RoleManager.class );
        final RoleInfo role = roleManager.getRoleByType( roleType );
        if( role == null )
        {
            final String message = REZ.getString( "container.unknown-role-type.error", roleType.getName() );
            throw new TaskException( message );
        }
        return role;
    }

    /**
     * Locates a type factory.
     */
    protected final TypeFactory getTypeFactory( final String roleName )
        throws TaskException
    {
        try
        {
            final TypeManager typeManager = (TypeManager)getService( TypeManager.class );
            return typeManager.getFactory( roleName );
        }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "container.no-factory.error", roleName );
            throw new TaskException( message, te );
        }
    }
}
