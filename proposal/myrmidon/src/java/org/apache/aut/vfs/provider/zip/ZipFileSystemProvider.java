/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.zip;

import java.io.File;
import org.apache.aut.vfs.FileConstants;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystemProvider;
import org.apache.aut.vfs.provider.DefaultFileName;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A file system provider for Zip/Jar files.  Provides read-only file
 * systems, for local Zip files only.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="file-system" name="zip"
 */
public class ZipFileSystemProvider
    extends AbstractFileSystemProvider
    implements FileSystemProvider
{
    private final ZipFileNameParser m_parser = new ZipFileNameParser();

    /**
     * Parses a URI into its components.
     */
    protected ParsedUri parseUri( final FileObject baseFile,
                                  final String uriStr )
        throws FileSystemException
    {
        // Parse the URI
        final ParsedZipUri uri = m_parser.parseZipUri( uriStr );

        // Make the URI canonical

        // Resolve the Zip file name
        final String fileName = uri.getZipFileName();
        final FileObject file = getContext().resolveFile( baseFile, fileName );
        uri.setZipFile( file );

        // Rebuild the root URI
        final String rootUri = m_parser.buildRootUri( uri );
        uri.setRootUri( rootUri );

        return uri;
    }

    /**
     * Builds the URI for the root of a layered file system.
     */
    protected ParsedUri buildUri( final String scheme,
                                  final FileObject file )
        throws FileSystemException
    {
        ParsedZipUri uri = new ParsedZipUri();
        uri.setScheme( scheme );
        uri.setZipFile( file );
        final String rootUri = m_parser.buildRootUri( uri );
        uri.setRootUri( rootUri );
        uri.setPath( "/" );
        return uri;
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( final ParsedUri uri )
        throws FileSystemException
    {
        final ParsedZipUri zipUri = (ParsedZipUri)uri;
        final FileObject file = zipUri.getZipFile();

        // Make a local copy of the file
        final File zipFile = file.replicateFile( FileConstants.SELECT_SELF );

        // Create the file system
        DefaultFileName name = new DefaultFileName( m_parser, zipUri.getRootUri(), "/" );
        return new ZipFileSystem( getContext(), name, zipFile );
    }

}
