/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * This task loads properties from a property file and places them in the context.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="load-properties"
 */
public class LoadProperties
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( LoadProperties.class );

    private String m_prefix;
    private File m_file;

    /**
     * Specify the prefix to be placed before all properties (if any).
     */
    public void setPrefix( final String prefix )
    {
        m_prefix = prefix;
    }

    public void setFile( final File file )
    {
        m_file = file;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_file )
        {
            final String message = REZ.getString( "loadprop.no-file.error" );
            throw new TaskException( message );
        }

        //Make sure prefix ends with a '.' if specified
        if( null == m_prefix )
        {
            m_prefix = "";
        }
        else if( !m_prefix.endsWith( "." ) )
        {
            m_prefix += ".";
        }

        loadFile( m_file );
    }

    /**
     * Utility method to load properties file.
     */
    private void loadFile( final File file )
        throws TaskException
    {
        if( getLogger().isDebugEnabled() )
        {
            final String message =
                REZ.getString( "loadprop.file.notice", file.getAbsolutePath() );
            getLogger().debug( message );
        }

        if( !file.exists() )
        {
            final String message =
                REZ.getString( "loadprop.missing-file.notice", file.getAbsolutePath() );
            getLogger().debug( message );
        }
        else
        {
            FileInputStream input = null;

            try
            {
                input = new FileInputStream( file );
                final Properties properties = new PropertyLoader( this );
                properties.load( input );
            }
            catch( final IOException ioe )
            {
                throw new TaskException( ioe.getMessage(), ioe );
            }

            IOUtil.shutdownStream( input );
        }
    }

    /**
     * Utility method that will resolve and add specified proeprty.
     * Used by external PropertyLoader class as a call back method.
     */
    protected final void addUnresolvedValue( final String name, final String value )
    {
        try
        {
            final Object objectValue = resolveValue( value.toString() );
            final String name1 = m_prefix + name;
            getContext().setProperty( name1, objectValue );
        }
        catch( final TaskException te )
        {
            final String message = REZ.getString( "loadprop.bad-resolve.error", name, value );
            getLogger().info( message, te );
        }
    }
}
