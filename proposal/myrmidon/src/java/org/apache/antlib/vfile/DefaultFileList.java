/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A compound file list, which is made up of several other file lists.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 *
 * @ant:data-type name="v-path"
 * @ant:type type="v-path" name="v-path"
 */
public class DefaultFileList implements FileList
{
    private final List m_elements = new ArrayList();

    /**
     * Adds a single file to this list.
     */
    public void addLocation( final FileObject file )
    {
        final SingletonFileList element = new SingletonFileList();
        element.setFile( file );
        m_elements.add( element );
    }

    /**
     * Adds a path to this list.
     */
    public void addPath( final String pathStr )
    {
        final PathFileList path = new PathFileList();
        path.setPath( pathStr );
        m_elements.add( path );
    }

    /**
     * Adds a file list to this list.
     */
    public void add( final FileList list )
    {
        m_elements.add( list );
    }

    /**
     * Returns the list of files.
     */
    public FileObject[] listFiles( TaskContext context ) throws TaskException
    {
        // Collect the files from all elements
        final ArrayList allFiles = new ArrayList();
        for( Iterator iterator = m_elements.iterator(); iterator.hasNext(); )
        {
            FileList fileList = (FileList)iterator.next();
            FileObject[] files = fileList.listFiles( context );
            for( int i = 0; i < files.length; i++ )
            {
                FileObject file = files[ i ];
                allFiles.add( file );
            }
        }

        // Convert to array
        return (FileObject[])allFiles.toArray( new FileObject[ allFiles.size() ] );
    }
}
