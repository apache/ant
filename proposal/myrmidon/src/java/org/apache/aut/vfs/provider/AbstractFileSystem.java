/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import java.util.HashMap;
import java.util.Map;
import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;

/**
 * A partial file system implementation.
 *
 * @author Adam Murdoch
 */
public abstract class AbstractFileSystem implements FileSystem
{
    private FileObject m_root;
    private FileName m_rootName;

    /** Map from absolute file path to FileObject. */
    private Map m_files = new HashMap();

    protected AbstractFileSystem( FileName rootName )
    {
        m_rootName = rootName;
    }

    /**
     * Creates a file object.  This method is called only if the requested
     * file is not cached.
     */
    protected abstract FileObject createFile( FileName name ) throws FileSystemException;

    /**
     * Adds a file object to the cache.
     */
    protected void putFile( FileObject file )
    {
        m_files.put( file.getName().getPath(), file );
    }

    /**
     * Returns a cached file.
     */
    protected FileObject getFile( FileName name )
    {
        return (FileObject)m_files.get( name.getPath() );
    }

    /**
     * Returns the root file of this file system.
     */
    public FileObject getRoot() throws FileSystemException
    {
        if( m_root == null )
        {
            m_root = findFile( m_rootName );
        }
        return m_root;
    }

    /**
     * Finds a file in this file system.
     */
    public FileObject findFile( String nameStr ) throws FileSystemException
    {
        // Resolve the name, and create the file
        FileName name = m_rootName.resolveName( nameStr );
        return findFile( name );
    }

    /**
     * Finds a file in this file system.
     */
    public FileObject findFile( FileName name ) throws FileSystemException
    {
        // TODO - assert that name is from this file system
        FileObject file = (FileObject)m_files.get( name.getPath() );
        if( file == null )
        {
            file = createFile( name );
            m_files.put( name.getPath(), file );
        }
        return file;
    }
}
