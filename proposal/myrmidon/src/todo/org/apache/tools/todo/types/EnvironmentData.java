/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.util.ArrayList;
import java.util.Properties;
import org.apache.myrmidon.api.TaskException;

/**
 * Wrapper for environment variables.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class EnvironmentData
{
    protected final ArrayList m_variables = new ArrayList();

    public Properties getVariables()
    {
        final Properties environment = new Properties();
        final int size = m_variables.size();
        for( int i = 0; i < size; i++ )
        {
            final EnvironmentVariable variable = (EnvironmentVariable)m_variables.get( i );
            environment.setProperty( variable.getKey(), variable.getValue() );
        }
        return environment;
    }

    public void addVariable( EnvironmentVariable var )
    {
        m_variables.add( var );
    }

    public void addVariable( String key, String value )
    {
        final EnvironmentVariable var = new EnvironmentVariable();
        var.setKey( key );
        var.setValue( value );
        addVariable( var );
    }

    public void addVariables( EnvironmentData properties )
    {
        m_variables.addAll( properties.m_variables );
    }

    public int size()
    {
        return m_variables.size();
    }
}
