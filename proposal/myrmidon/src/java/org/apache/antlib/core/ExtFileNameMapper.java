/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileNameMapper;

/**
 * Maps file extensions.
 *
 * @ant.type type="mapper" name="map-extension"
 */
public class ExtFileNameMapper
    implements FileNameMapper
{
    private String m_extension;

    public void setExtension( final String extension )
    {
        m_extension = extension;
    }

    public String[] mapFileName( final String filename, TaskContext context )
        throws TaskException
    {
        final String name = FileUtil.removeExtension( filename );
        if( m_extension != null )
        {
            return new String[]{ name + '.' + m_extension };
        }
        else
        {
            return new String[]{ name };
        }
    }
}
