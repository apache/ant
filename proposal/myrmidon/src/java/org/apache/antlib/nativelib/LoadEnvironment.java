/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.nativelib;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.nativelib.Environment;
import org.apache.aut.nativelib.ExecException;

/**
 * This task is responsible for loading that OS-specific environment
 * variables and adding them as propertys to the context with a specified
 * prefix.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class LoadEnvironment
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( LoadEnvironment.class );

    private String m_prefix;

    public void setPrefix( final String prefix )
    {
        m_prefix = prefix;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_prefix )
        {
            final String message = REZ.getString( "loadenv.no-prefix.error" );
            throw new TaskException( message );
        }

        //Make sure prefix ends with a '.'
        if( !m_prefix.endsWith( "." ) )
        {
            m_prefix += ".";
        }

        if( getLogger().isDebugEnabled() )
        {
            final String displayPrefix =
                m_prefix.substring( 0, m_prefix.length() - 1 );
            final String message =
                REZ.getString( "loadenv.prefix.notice", displayPrefix );
            getLogger().debug( message );
        }

        final Properties environment = loadNativeEnvironment();
        final Iterator keys = environment.keySet().iterator();
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final String value = environment.getProperty( key );

            if( value.equals( "" ) )
            {
                final String message = REZ.getString( "loadenv.ignoring-empty.warn", key );
                getLogger().warn( message );
            }
            else
            {
                getContext().setProperty( m_prefix + key, value );
            }
        }
    }

    /**
     * Utility method to load the native environment variables.
     */
    private Properties loadNativeEnvironment()
        throws TaskException
    {
        try
        {
            return Environment.getNativeEnvironment();
        }
        catch( final ExecException ee )
        {
            throw new TaskException( ee.getMessage(), ee );
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }
    }
}
