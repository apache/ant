/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.file;

import java.io.File;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Creates a given directory.
 *
 * @author duncan@x180.com
 */
public class Mkdir
    extends AbstractTask
{
    private File m_dir;

    public void setDir( final File dir )
    {
        m_dir = dir;
    }

    public void execute()
        throws TaskException
    {
        if( m_dir == null )
        {
            final String message = "dir attribute is required";
            throw new TaskException( message );
        }

        if( m_dir.isFile() )
        {
            final String message = "Unable to create directory as a file " +
                "already exists with that name: " + m_dir.getAbsolutePath();
            throw new TaskException( message );
        }

        if( !m_dir.exists() )
        {
            boolean result = m_dir.mkdirs();
            if( result == false )
            {
                final String message = "Directory " + m_dir.getAbsolutePath() + " creation was not " +
                    "successful for an unknown reason";
                throw new TaskException( message );
            }
            final String message = "Created dir: " + m_dir.getAbsolutePath();
            getLogger().info( message );
        }
    }
}
