/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import java.io.File;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.property.PropertyResolver;
import org.apache.myrmidon.interfaces.store.PropertyStore;

/**
 * Default implementation of TaskContext.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultTaskContext
    implements TaskContext
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultTaskContext.class );

    private final ServiceManager m_serviceManager;
    private final Logger m_logger;
    private final PropertyStore m_store;
    private PropertyResolver m_propertyResolver;

    /**
     * Constructor that takes both parent context and a service directory.
     */
    public DefaultTaskContext( final ServiceManager serviceManager,
                               final Logger logger,
                               final PropertyStore store )
        throws TaskException
    {
        m_serviceManager = serviceManager;
        m_logger = logger;
        m_store = store;

        if( null == m_serviceManager )
        {
            throw new NullPointerException( "serviceManager" );
        }
        if( null == m_logger )
        {
            throw new NullPointerException( "logger" );
        }
        if( null == m_store )
        {
            throw new NullPointerException( "store" );
        }
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
        final String name = serviceClass.getName();
        //Note that this will chain up to parent ServiceManagers (if any)
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
            if( null == m_propertyResolver )
            {
                m_propertyResolver = (PropertyResolver)getService( PropertyResolver.class );
            }
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
        try
        {
            return m_store.getProperty( name );
        }
        catch( final Exception e )
        {
            return null;
        }
    }

    /**
     * Retrieve a copy of all the properties accessible via context.
     *
     * @return the map of all property names to values
     */
    public Map getProperties()
        throws TaskException
    {
        try
        {
            return m_store.getProperties();
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
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
        try
        {
            m_store.setProperty( name, value );
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
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
     * Log a verbose message.
     *
     * @param message the message
     */
    public void verbose( String message )
    {
        m_logger.info( message );
    }

    /**
     * Log a verbose message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void verbose( String message, Throwable throwable )
    {
        m_logger.info( message, throwable );
    }

    /**
     * Determine if messages of priority "verbose" will be logged.
     *
     * @return true if "verbose" messages will be logged
     */
    public boolean isVerboseEnabled()
    {
        return m_logger.isInfoEnabled();
    }

    /**
     * Log a info message.
     *
     * @param message the message
     */
    public void info( final String message )
    {
        m_logger.warn( message );
    }

    /**
     * Log a info message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void info( final String message, final Throwable throwable )
    {
        m_logger.warn( message, throwable );
    }

    /**
     * Determine if messages of priority "info" will be logged.
     *
     * @return true if "info" messages will be logged
     */
    public boolean isInfoEnabled()
    {
        return m_logger.isWarnEnabled();
    }

    /**
     * Log a warn message.
     *
     * @param message the message
     */
    public void warn( final String message )
    {
        m_logger.error( message );
    }

    /**
     * Log a warn message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void warn( final String message, final Throwable throwable )
    {
        m_logger.error( message, throwable );
    }

    /**
     * Determine if messages of priority "warn" will be logged.
     *
     * @return true if "warn" messages will be logged
     */
    public boolean isWarnEnabled()
    {
        return m_logger.isErrorEnabled();
    }

    /**
     * Log a error message.
     *
     * @param message the message
     */
    public void error( final String message )
    {
        m_logger.fatalError( message );
    }

    /**
     * Log a error message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void error( final String message, final Throwable throwable )
    {
        m_logger.fatalError( message, throwable );
    }

    /**
     * Determine if messages of priority "error" will be logged.
     *
     * @return true if "error" messages will be logged
     */
    public boolean isErrorEnabled()
    {
        return m_logger.isFatalErrorEnabled();
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
        try
        {
            final PropertyStore store = m_store.createChildStore( name );
            final DefaultServiceManager serviceManager =
                new DefaultServiceManager( m_serviceManager );
            final Logger logger = m_logger.getChildLogger( name );

            return new DefaultTaskContext( serviceManager, logger, store );
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
