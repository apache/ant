/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file.test;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.FileList;
import org.apache.myrmidon.framework.file.Path;

/**
 * A test FileList implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="path" name="test-file-list"
 */
public class TestFileList
    implements FileList
{
    private String m_name;
    private Path m_path;

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setPath( final Path path )
    {
        m_path = path;
    }

    /**
     * Returns the files in this list.
     */
    public String[] listFiles( final TaskContext context )
        throws TaskException
    {
        final ArrayList files = new ArrayList();
        if( m_name != null )
        {
            final File file = context.resolveFile( m_name );
            files.add( file.getAbsolutePath() );
        }
        if( m_path != null )
        {
            final String[] fileNames = m_path.listFiles( context );
            for( int i = 0; i < fileNames.length; i++ )
            {
                files.add( fileNames[ i ] );
            }
        }
        return (String[])files.toArray( new String[ files.size() ] );
    }
}
