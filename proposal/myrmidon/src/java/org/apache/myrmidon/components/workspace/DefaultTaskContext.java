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
import org.apache.avalon.excalibur.property.PropertyException;
import org.apache.avalon.excalibur.property.PropertyUtil;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Default implementation of TaskContext.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultTaskContext
    extends DefaultContext
    implements TaskContext
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultTaskContext.class );

    /**
     * Constructor for Context with no parent contexts.
     */
    public DefaultTaskContext( final Map contextData )
    {
        super( contextData );
    }

    /**
     * Constructor for Context with no parent contexts.
     */
    public DefaultTaskContext()
    {
        this( (TaskContext)null );
    }

    /**
     * Constructor.
     */
    public DefaultTaskContext( final TaskContext parent )
    {
        super( parent );
    }

    /**
     * Retrieve Name of tasklet.
     *
     * @return the name
     */
    public String getName()
    {
        try
        {
            return (String)get( NAME );
        }
        catch( final ContextException ce )
        {
            final String message = REZ.getString( "no-name.error" );
            throw new IllegalStateException( message );
        }
    }

    /**
     * Retrieve base directory.
     *
     * @return the base directory
     */
    public File getBaseDirectory()
    {
        try
        {
            return (File)get( BASE_DIRECTORY );
        }
        catch( final ContextException ce )
        {
            final String message = REZ.getString( "no-dir.error" );
            throw new IllegalStateException( message );
        }
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
        catch( final PropertyException pe )
        {
            final String message = REZ.getString( "bad-resolve.error", value );
            throw new TaskException( message, pe );
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
            return get( name );
        }
        catch( final ContextException ce )
        {
            return null;
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
        setProperty( name, value, CURRENT );
    }

    /**
     * Set property value.
     *
     * @param property the property
     */
    public void setProperty( final String name, final Object value, final ScopeEnum scope )
        throws TaskException
    {
        checkPropertyValid( name, value );

        if( CURRENT == scope ) {
            put( name, value );
        } else if( PARENT == scope )
        {
            if( null == getParent() )
            {
                final String message = REZ.getString( "no-parent.error" );
                throw new TaskException( message );
            }
            else
            {
                ( (TaskContext)getParent() ).setProperty( name, value );
            }
        }
        else if( TOP_LEVEL == scope )
        {
            DefaultTaskContext context = this;

            while( null != context.getParent() )
            {
                context = (DefaultTaskContext)context.getParent();
            }

            context.put( name, value );
        }
        else
        {
            final String message = REZ.getString( "bad-scope.error", scope );
            throw new IllegalStateException( message );
        }
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
