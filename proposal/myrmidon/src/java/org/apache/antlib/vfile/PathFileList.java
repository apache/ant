/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileSystemManager;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.util.FileUtils;

/**
 * A path made up of file names separated by ; and : characters.  Similar to
 * a CLASSPATH or PATH environment variable.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class PathFileList implements FileList
{
    private String m_path;

    /**
     * Sets the path to use for this file list.
     */
    public void setPath( final String path )
    {
        m_path = path;
    }

    /**
     * Returns the list of files.
     */
    public FileObject[] listFiles( final TaskContext context )
        throws TaskException
    {
        FileSystemManager fileSystemManager = (FileSystemManager)context.getService( FileSystemManager.class );

        // TODO - move parsing to the VFS
        final String[] elements = FileUtils.parsePath( m_path );
        final FileObject[] result = new FileObject[ elements.length ];
        for( int i = 0; i < elements.length; i++ )
        {
            String element = elements[ i ];
            try
            {
                result[ i ] = fileSystemManager.resolveFile( element );
            }
            catch( FileSystemException e )
            {
                throw new TaskException( e.getMessage(), e );
            }
        }

        return result;
    }
}
