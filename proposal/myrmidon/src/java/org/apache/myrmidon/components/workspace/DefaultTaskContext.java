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
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Default implementation of TaskContext.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultTaskContext
    implements TaskContext
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultTaskContext.class );

    private final Map m_contextData = new Hashtable();
    private final TaskContext m_parent;
    private ServiceManager m_serviceManager;

    /**
     * Constructor for Context with no parent contexts.
     */
    public DefaultTaskContext()
    {
        this( null, null );
    }

    /**
     * Constructor that specified parent context.
     */
    public DefaultTaskContext( final TaskContext parent )
    {
        this( parent, null );
    }

    /**
     * Constructor that specifies the service directory for context.
     */
    public DefaultTaskContext( final ServiceManager serviceManager )
    {
        this( null, serviceManager );
    }

    /**
     * Constructor that takes both parent context and a service directory.
     */
    public DefaultTaskContext( final TaskContext parent,
                               final ServiceManager serviceManager )
    {
        m_parent = parent;
        m_serviceManager = serviceManager;
    }

    /**
     * Retrieve a property.
     */
    private Object get( final String key )
    {
        final Object data = m_contextData.get( key );
        if( null != data )
        {
            //            if( data instanceof Resolvable )
            //            {
            //                return ( (Resolvable)data ).resolve( this );
            //            }
            return data;
        }

        // If data was null, check the parent
        if( null == m_parent )
        {
            // There was no parent, and no data
            return null;
        }

        return m_parent.getProperty( key );
    }

    /**
     * Retrieve Name of task.
     *
     * @return the name
     */
    public String getName()
    {
        return (String)get( NAME );
    }

    /**
     * Retrieve base directory.
     *
     * @return the base directory
     */
    public File getBaseDirectory()
    {
        return (File)get( BASE_DIRECTORY );
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
        if( m_serviceManager != null && m_serviceManager.hasService( name ) )
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
                PropertyUtil.resolveProperty( value, this, false );

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
        return get( name );
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
        checkPropertyValid( name, value );
        m_contextData.put( name, value );
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
        final DefaultTaskContext context = new DefaultTaskContext( this );

        context.setProperty( TaskContext.NAME, getName() + "." + name );
        context.setProperty( TaskContext.BASE_DIRECTORY, getBaseDirectory() );

        return context;
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
