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
import org.apache.aut.nativelib.ExecException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.EnvironmentData;
import org.apache.tools.todo.types.EnvironmentVariable;

/**
 * Specialized EnvironmentData class for System properties
 */
public class SysProperties
    extends EnvironmentData
    implements Cloneable
{
    private Properties m_system;

    public void setSystem()
        throws TaskException
    {
        try
        {
            Properties p = new Properties( m_system = System.getProperties() );

            for( Iterator e = m_variables.iterator(); e.hasNext(); )
            {
                EnvironmentVariable v = (EnvironmentVariable)e.next();
                p.put( v.getKey(), v.getValue() );
            }
            System.setProperties( p );
        }
        catch( SecurityException e )
        {
            throw new TaskException( "Cannot modify system properties", e );
        }
    }

    /**
     * @todo move this to AUT
     */
    public String[] getJavaVariables()
        throws TaskException
    {
        String props[] = toNativeFormat( super.getVariables() );
        for( int i = 0; i < props.length; i++ )
        {
            props[ i ] = "-D" + props[ i ];
        }
        return props;
    }

    public Object clone()
    {
        try
        {
            SysProperties c = (SysProperties)super.clone();
            c.m_variables.addAll( (ArrayList)m_variables.clone() );
            return c;
        }
        catch( CloneNotSupportedException e )
        {
            return null;
        }
    }

    public void restoreSystem()
        throws TaskException
    {
        if( m_system == null )
        {
            throw new TaskException( "Unbalanced nesting of SysProperties" );
        }

        try
        {
            System.setProperties( m_system );
            m_system = null;
        }
        catch( SecurityException e )
        {
            throw new TaskException( "Cannot modify system properties", e );
        }
    }

    public int size()
    {
        return m_variables.size();
    }

    private String[] toNativeFormat( final Properties environment )
    {
        final ArrayList newEnvironment = new ArrayList();

        final Iterator keys = environment.keySet().iterator();
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final String value = environment.getProperty( key );
            newEnvironment.add( key + '=' + value );
        }

        return (String[])newEnvironment.toArray( new String[ newEnvironment.size() ] );
    }
}
