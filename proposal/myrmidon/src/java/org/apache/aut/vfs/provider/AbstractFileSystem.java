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
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * A partial file system implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractFileSystem
    extends AbstractLogEnabled
    implements FileSystem, Disposable
{
    private FileObject m_root;
    private final FileName m_rootName;
    private final FileSystemProviderContext m_context;

    /** Map from FileName to FileObject. */
    private final Map m_files = new HashMap();

    protected AbstractFileSystem( final FileSystemProviderContext context,
                                  final FileName rootName )
    {
        m_rootName = rootName;
        m_context = context;
    }

    public void dispose()
    {
        // Clean-up
        m_files.clear();
    }

    /**
     * Creates a file object.  This method is called only if the requested
     * file is not cached.
     */
    protected abstract FileObject createFile( final FileName name ) throws FileSystemException;

    /**
     * Adds a file object to the cache.
     */
    protected void putFile( final FileObject file )
    {
        m_files.put( file.getName(), file );
    }

    /**
     * Returns a cached file.
     */
    protected FileObject getFile( final FileName name )
    {
        return (FileObject)m_files.get( name );
    }

    /**
     * Returns the context fir this file system.
     */
    public FileSystemProviderContext getContext()
    {
        return m_context;
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
    public FileObject findFile( final String nameStr ) throws FileSystemException
    {
        // Resolve the name, and create the file
        final FileName name = m_rootName.resolveName( nameStr );
        return findFile( name );
    }

    /**
     * Finds a file in this file system.
     */
    public FileObject findFile( final FileName name ) throws FileSystemException
    {
        // TODO - assert that name is from this file system
        FileObject file = (FileObject)m_files.get( name );
        if( file == null )
        {
            file = createFile( name );
            m_files.put( name, file );
        }
        return file;
    }
}
