/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.nativelib;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.Path;

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

    public void setPath( final Path path ) throws TaskException
    {
        throw new TaskException( "Using a path not implemented." );
        //m_value = PathUtil.formatPath( path );
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
