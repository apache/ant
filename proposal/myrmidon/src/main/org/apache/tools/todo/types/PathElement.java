/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.util.FileUtils;

/**
 * Helper class, holds <code>&lt;&gt;</code> values.
 */
class PathElement
{
    private String m_location;
    private String m_path;

    public void setLocation( final File location )
    {
        m_location = location.getAbsolutePath();
    }

    public void setPath( String path )
    {
        m_path = path;
    }

    protected String[] getParts( final File baseDirectory )
        throws TaskException
    {
        if( m_location != null )
        {
            return new String[]{m_location};
        }
        return FileUtils.translatePath( baseDirectory, m_path );
    }
}
