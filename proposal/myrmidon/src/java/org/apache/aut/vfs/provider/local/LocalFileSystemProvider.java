/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystemProvider;
import org.apache.aut.vfs.provider.DefaultFileName;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A file system provider, which uses direct file access.
 *
 * @author Adam Murdoch
 */
public class LocalFileSystemProvider extends AbstractFileSystemProvider
    implements FileSystemProvider
{
    private LocalFileNameParser m_parser = new LocalFileNameParser();

    /**
     * Determines if a name is an absolute file name.
     */
    public boolean isAbsoluteLocalName( String name )
    {
        return m_parser.isAbsoluteName( name );
    }

    /**
     * Finds a file by local file name.
     */
    public FileObject findFileByLocalName( String name ) throws FileSystemException
    {
        // TODO - tidy this up, no need to turn the name into an absolute URI,
        // and then straight back again
        return findFile( "file:" + name );
    }

    /**
     * Parses a URI into its components.  The returned value is used to
     * locate the file system in the cache (using the root prefix), and is
     * passed to {@link #createFileSystem} to create the file system.
     *
     * <p>The provider can annotate this object with any additional
     * information it requires to create a file system from the URI.
     */
    protected ParsedUri parseURI( String uri ) throws FileSystemException
    {
        return m_parser.parseUri( uri );
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( ParsedUri uri ) throws FileSystemException
    {
        // Build the name of the root file.
        ParsedFileUri fileUri = (ParsedFileUri)uri;
        String rootFile = fileUri.getRootFile();

        // Create the file system
        DefaultFileName rootName = new DefaultFileName( m_parser, fileUri.getRootURI(), "/" );
        return new LocalFileSystem( rootName, rootFile );
    }
}
