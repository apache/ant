/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Path;

public class DocletInfo
{
    private ArrayList m_params = new ArrayList();
    private String m_name;
    private Path m_path;

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setPath( final Path path )
        throws TaskException
    {
        if( m_path == null )
        {
            m_path = path;
        }
        else
        {
            m_path.append( path );
        }
    }

    public String getName()
    {
        return m_name;
    }

    public Iterator getParams()
    {
        return m_params.iterator();
    }

    public Path getPath()
    {
        return m_path;
    }

    public DocletParam createParam()
    {
        final DocletParam param = new DocletParam();
        m_params.add( param );
        return param;
    }

    public Path createPath()
        throws TaskException
    {
        if( m_path == null )
        {
            m_path = new Path();
        }
        return m_path.createPath();
    }
}
