/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
import java.util.List;
import org.apache.aut.vfs.FileObject;

/**
 * An implementation of a file set result.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class DefaultFileSetResult
    implements FileSetResult
{
    private List m_files = new ArrayList();
    private List m_paths = new ArrayList();

    /**
     * Adds an element to the result.
     */
    public void addElement( final FileObject file,
                            final String path )
    {
        m_files.add( file );
        m_paths.add( path );
    }

    /**
     * Returns the files in the result.
     */
    public FileObject[] getFiles()
    {
        return (FileObject[])m_files.toArray( new FileObject[ m_files.size() ] );
    }

    /**
     * Returns the virtual paths of the files.
     */
    public String[] getPaths()
    {
        return (String[])m_paths.toArray( new String[ m_paths.size() ] );
    }
}
