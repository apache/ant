/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.model.DefaultNameValidator;
import org.apache.myrmidon.interfaces.workspace.PropertyResolver;

/**
 * Default implementation of TaskContext.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultTaskContext
    implements TaskContext, Context
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultTaskContext.class );

    // Property name validator allows digits, but no internal whitespace.
    private static DefaultNameValidator c_propertyNameValidator = new DefaultNameValidator();

    static
    {
        c_propertyNameValidator.setAllowInternalWhitespace( false );
    }

    private final Map m_contextData = new Hashtable();
    private final TaskContext m_parent;
    private final ServiceManager m_serviceManager;
    private final Logger m_logger;
    private final PropertyResolver m_propertyResolver;

    /**
     * Constructor that takes both parent context and a service directory.
     */
    public DefaultTaskContext( final TaskContext parent,
                               final ServiceManager serviceManager,
                               final Logger logger )
        throws TaskException
    {
        m_parent = parent;
        m_serviceManager = serviceManager;
        m_logger = logger;
        m_propertyResolver = (PropertyResolver)getService( PropertyResolver.class );
    }

    /**
     * Retrieve Name of task.
     *
     * @return the name
     */
    public String getName()
    {
        return (String)getProperty( NAME );
    }

    /**
     * Retrieve base directory.
     *
     * @return the base directory
     */
    public File getBaseDirectory()
    {
        return (File)getProperty( BASE_DIRECTORY );
    }

    /**
     * Retrieve a service that is offered by the runtime.
     * The actual services registered and in place for the
     * task is determined by the container. The returned service
     * <b>MUST</b> implement the specified interface.
     *
     * @param serviceClass the interface class that defines the service
     * @return an instance of the service implementing interface specified by parameter
     * @exception TaskException is thrown when the service is unavailable or not supported
     */
    public Object getService( final Class serviceClass )
        throws TaskException
    {
        // Try this context first
        final String name = serviceClass.getName();
        if( null != m_serviceManager && m_serviceManager.hasService( name ) )
        {
            try
            {
                return m_serviceManager.lookup( name );
            }
            catch( final ServiceException se )
            {
                throw new TaskException( se.getMessage(), se );
            }
        }

        // Try parent
        if( null != m_parent )
        {
            return m_parent.getService( serviceClass );
        }

        // Not found
        final String message = REZ.getString( "bad-find-service.error", name );
        throw new TaskException( message );
    }

    /**
     * Resolve filename.
     * This involves resolving it against baseDirectory and
     * removing ../ and ./ references. It also means formatting
     * it appropriately for the particular OS (ie different OS have
     * different volumes, file conventions etc)
     *
     * @param filename the filename to resolve
     * @return the resolved filename
     */
    public File resolveFile( final String filename )
    {
        return FileUtil.resolveFile( getBaseDirectory(), filename );
    }

    /**
     * Resolve a value according to the context.
     * This involves evaluating the string and thus removing
     * ${} sequences according to the rules specified at
     * ............
     *
     * @param value the value to resolve
     * @return the resolved value
     */
    public Object resolveValue( final String value )
        throws TaskException
    {
        try
        {
            final Object object =
                m_propertyResolver.resolveProperties( value, this );
            if( null == object )
            {
                final String message = REZ.getString( "null-resolved-value.error", value );
                throw new TaskException( message );
            }

            return object;
        }
        catch( final TaskException te )
        {
            final String message = REZ.getString( "bad-resolve.error", value );
            throw new TaskException( message, te );
        }
    }

    /**
     * Retrieve property for name.
     *
     * @param name the name of property
     * @return the value of the property
     */
    public Object getProperty( final String name )
    {
        Object value = m_contextData.get( name );
        if( value == null && m_parent != null )
        {
            value = m_parent.getProperty( name );
        }
        return value;
    }

    /**
     * Retrieve a copy of all the properties accessible via context.
     *
     * @return the map of all property names to values
     */
    public Map getProperties()
    {
        return null;
    }

    /**
     * Set property value in current context.
     *
     * @param name the name of property
     * @param value the value of property
     */
    public void setProperty( final String name, final Object value )
        throws TaskException
    {
        checkPropertyName( name );
        checkPropertyValid( name, value );
        m_contextData.put( name, value );
    }

    /**
     * Log a debug message.
     *
     * @param message the message
     */
    public void debug( final String message )
    {
        m_logger.debug( message );
    }

    /**
     * Log a debug message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void debug( final String message, final Throwable throwable )
    {
        m_logger.debug( message, throwable );
    }

    /**
     * Determine if messages of priority "debug" will be logged.
     *
     * @return true if "debug" messages will be logged
     */
    public boolean isDebugEnabled()
    {
        return m_logger.isDebugEnabled();
    }

    /**
     * Log a info message.
     *
     * @param message the message
     */
    public void info( final String message )
    {
        m_logger.info( message );
    }

    /**
     * Log a info message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void info( final String message, final Throwable throwable )
    {
        m_logger.info( message, throwable );
    }

    /**
     * Determine if messages of priority "info" will be logged.
     *
     * @return true if "info" messages will be logged
     */
    public boolean isInfoEnabled()
    {
        return m_logger.isInfoEnabled();
    }

    /**
     * Log a warn message.
     *
     * @param message the message
     */
    public void warn( final String message )
    {
        m_logger.warn( message );
    }

    /**
     * Log a warn message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void warn( final String message, final Throwable throwable )
    {
        m_logger.warn( message, throwable );
    }

    /**
     * Determine if messages of priority "warn" will be logged.
     *
     * @return true if "warn" messages will be logged
     */
    public boolean isWarnEnabled()
    {
        return m_logger.isWarnEnabled();
    }

    /**
     * Log a error message.
     *
     * @param message the message
     */
    public void error( final String message )
    {
        m_logger.error( message );
    }

    /**
     * Log a error message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void error( final String message, final Throwable throwable )
    {
        m_logger.error( message, throwable );
    }

    /**
     * Determine if messages of priority "error" will be logged.
     *
     * @return true if "error" messages will be logged
     */
    public boolean isErrorEnabled()
    {
        return m_logger.isErrorEnabled();
    }

    /**
     * Create a Child Context.
     * This allows separate hierarchly contexts to be easily constructed.
     *
     * @param name the name of sub-context
     * @return the created TaskContext
     * @exception TaskException if an error occurs
     */
    public TaskContext createSubContext( final String name )
        throws TaskException
    {
        final DefaultTaskContext context =
            new DefaultTaskContext( this, m_serviceManager, m_logger );

        context.setProperty( TaskContext.NAME, getName() + "." + name );
        context.setProperty( TaskContext.BASE_DIRECTORY, getBaseDirectory() );

        return context;
    }

    /**
     * Returns a property.
     */
    public Object get( final Object key ) throws ContextException
    {
        final Object value = getProperty( (String)key );
        if( value == null )
        {
            final String message = REZ.getString( "unknown-property.error", key );
            throw new ContextException( message );
        }
        return value;
    }

    /**
     * Checks that the supplied property name is valid.
     */
    private void checkPropertyName( final String name ) throws TaskException
    {
        try
        {
            c_propertyNameValidator.validate( name );
        }
        catch( Exception e )
        {
            String message = REZ.getString( "bad-property-name.error" );
            throw new TaskException( message, e );
        }
    }

    /**
     * Make sure property is valid if it is one of the "magic" properties.
     *
     * @param name the name of property
     * @param value the value of proeprty
     * @exception TaskException if an error occurs
     */
    protected void checkPropertyValid( final String name, final Object value )
        throws TaskException
    {
        if( BASE_DIRECTORY.equals( name ) && !( value instanceof File ) )
        {
            final String message =
                REZ.getString( "bad-property.error", BASE_DIRECTORY, File.class.getName() );
            throw new TaskException( message );
        }
        else if( NAME.equals( name ) && !( value instanceof String ) )
        {
            final String message =
                REZ.getString( "bad-property.error", NAME, String.class.getName() );
            throw new TaskException( message );
        }
    }
}
