/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.nativelib.EnvironmentData;

/**
 * A utility class for handling System properties
 *
 * @todo move this to AUT
 */
final public class SysProperties
{
    private static Properties m_system;

    private SysProperties()
    {
    }

    /**
     * Sets system properties.  The current set of system properties can be
     * restored using {@link #restoreSystem}.
     */
    public static void setSystem( final EnvironmentData properties )
        throws TaskException
    {
        setSystem( properties.getVariables() );
    }

    /**
     * Sets system properties.  The current set of system properties can be
     * restored using {@link #restoreSystem}.
     */
    public synchronized static void setSystem( final Map properties )
        throws TaskException
    {
        if( properties.size() == 0 )
        {
            return;
        }
        if( m_system != null )
        {
            throw new TaskException( "System properties have not been restored." );
        }

        final Properties sysProps;
        try
        {
            sysProps = System.getProperties();
            Properties allProps = new Properties( sysProps );
            allProps.putAll( properties );
            System.setProperties( allProps );
        }
        catch( final SecurityException e )
        {
            throw new TaskException( "Cannot modify system properties.", e );
        }

        m_system = sysProps;
    }

    /**
     * Restores the system properties to what they were before the last
     * call to {@link #setSystem}.
     */
    public static synchronized void restoreSystem()
        throws TaskException
    {
        if( m_system == null )
        {
            return;
        }

        try
        {
            System.setProperties( m_system );
            m_system = null;
        }
        catch( final SecurityException e )
        {
            throw new TaskException( "Cannot modify system properties.", e );
        }
    }

    /**
     * Converts a set of properties to their -D command-line equivalent.
     */
    public static String[] getJavaVariables( final EnvironmentData environment )
    {
        return getJavaVariables( environment.getVariables() );
    }

    /**
     * Converts a set of properties to their -D command-line equivalent.
     */
    public static String[] getJavaVariables( final Map environment )
    {
        final ArrayList vars = new ArrayList();

        final Iterator keys = environment.keySet().iterator();
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final Object value = environment.get( key );
            vars.add( "-D" + key + '=' + value );
        }

        return (String[])vars.toArray( new String[ vars.size() ] );
    }
}
