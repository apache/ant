/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.smb;

import java.io.InputStream;
import java.io.OutputStream;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.aut.vfs.provider.AbstractFileObject;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A file in an SMB file system.
 *
 * @author Adam Murdoch
 */
public class SmbFileObject extends AbstractFileObject implements FileObject
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( SmbFileObject.class );

    private String m_fileName;
    private SmbFile m_file;

    protected SmbFileObject( String fileName, FileName name, SmbFileSystem fs )
    {
        super( name, fs );
        m_fileName = fileName;
    }

    /**
     * Attaches this file object to its file resource.
     */
    protected void doAttach() throws Exception
    {
        // Defer creation of the SmbFile to here
        if( m_file == null )
        {
            m_file = new SmbFile( m_fileName );
        }
    }

    /**
     * Detaches this file object from its file resource.
     */
    protected void doDetach()
    {
        // Need to throw away the file when the file's type changes, because
        // the SmbFile caches the type
        m_file = null;
    }

    /**
     * Determines the type of the file, returns null if the file does not
     * exist.
     */
    protected FileType doGetType() throws Exception
    {
        // Need to check whether parent exists or not, because SmbFile.exists()
        // throws an exception if it does not
        // TODO - patch jCIFS?

        FileObject parent = getParent();
        if( parent != null && !parent.exists() )
        {
            return null;
        }

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
        final String message = REZ.getString( "get-type.error", getName() );
        throw new FileSystemException( message );
    }

    /**
     * Lists the children of the file.  Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.
     */
    protected String[] doListChildren() throws Exception
    {
        return m_file.list();
    }

    /**
     * Deletes the file.
     */
    protected void doDelete() throws Exception
    {
        m_file.delete();
    }

    /**
     * Creates this file as a folder.
     */
    protected void doCreateFolder() throws Exception
    {
        m_file.mkdir();
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    protected long doGetContentSize() throws Exception
    {
        return m_file.length();
    }

    /**
     * Creates an input stream to read the file content from.
     */
    protected InputStream doGetInputStream() throws Exception
    {
        return new SmbFileInputStream( m_file );
    }

    /**
     * Creates an output stream to write the file content to.
     */
    protected OutputStream doGetOutputStream() throws Exception
    {
        return new SmbFileOutputStream( m_file );
    }
}
