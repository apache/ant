/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.aut.vfs.provider.AbstractFileObject;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A file object implementation which uses direct file access.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
final class LocalFile
    extends AbstractFileObject
    implements FileObject
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( LocalFile.class );

    private File m_file;
    private final String m_fileName;

    /**
     * Creates a non-root file.
     */
    public LocalFile( final LocalFileSystem fileSystem,
                      final String fileName,
                      final FileName name )
    {
        super( name, fileSystem );
        m_fileName = fileName;
    }

    /**
     * Attaches this file object to its file resource.
     */
    protected void doAttach()
        throws Exception
    {
        if( m_file == null )
        {
            m_file = new File( m_fileName );
        }
    }

    /**
     * Returns the file's type.
     */
    protected FileType doGetType()
        throws Exception
    {
        if( !m_file.exists() )
        {
            return null;
        }
        if( m_file.isDirectory() )
        {
            return FileType.FOLDER;
        }
        if( m_file.isFile() )
        {
            return FileType.FILE;
        }

        final String message = REZ.getString( "get-type.error", m_file );
        throw new FileSystemException( message );
    }

    /**
     * Returns the children of the file.
     */
    protected String[] doListChildren()
        throws Exception
    {
        return m_file.list();
    }

    /**
     * Deletes this file, and all children.
     */
    public void doDelete()
        throws Exception
    {
        if( !m_file.delete() )
        {
            final String message = REZ.getString( "delete-file.error", m_file );
            throw new FileSystemException( message );
        }
    }

    /**
     * Creates this folder.
     */
    protected void doCreateFolder()
        throws Exception
    {
        if( !m_file.mkdir() )
        {
            final String message = REZ.getString( "create-folder.error", m_file );
            throw new FileSystemException( message );
        }
    }

    /**
     * Creates an input stream to read the content from.
     */
    protected InputStream doGetInputStream()
        throws Exception
    {
        return new FileInputStream( m_file );
    }

    /**
     * Creates an output stream to write the file content to.
     */
    protected OutputStream doGetOutputStream()
        throws Exception
    {
        return new FileOutputStream( m_file );
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    protected long doGetContentSize()
        throws Exception
    {
        return m_file.length();
    }
}
