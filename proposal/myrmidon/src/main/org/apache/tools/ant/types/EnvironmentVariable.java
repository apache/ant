/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;

public class EnvironmentVariable
{
    private String m_key;
    private String m_value;

    public void setFile( final File file )
    {
        m_value = file.getAbsolutePath();
    }

    public void setKey( final String key )
    {
        m_key = key;
    }

    public void setPath( final Path path )
    {
        m_value = path.toString();
    }

    public void setValue( final String value )
    {
        m_value = value;
    }

    public String getKey()
    {
        return m_key;
    }

    public String getValue()
    {
        return m_value;
    }
}
