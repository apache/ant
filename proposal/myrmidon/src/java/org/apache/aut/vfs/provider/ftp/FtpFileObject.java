/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.ftp;

import com.oroinc.net.ftp.FTPFile;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.aut.vfs.provider.AbstractFileObject;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * An FTP file.
 *
 * @author Adam Murdoch
 */
class FtpFileObject
    extends AbstractFileObject
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( FtpFileObject.class );

    private final static FTPFile[] EMPTY_FTP_FILE_ARRAY = {};

    private FtpFileSystem m_ftpFs;

    // Cached info
    private FTPFile m_fileInfo;
    private FTPFile[] m_children;


    public FtpFileObject( final FileName name, final FtpFileSystem fileSystem )
    {
        super( name, fileSystem );
        m_ftpFs = fileSystem;
    }

    /**
     * Called by child file objects, to locate their ftp file info.
     */
    private FTPFile getChildFile( String name ) throws Exception
    {
        if( m_children == null )
        {
            // List the children of this file
            m_children = m_ftpFs.getClient().listFiles( getName().getPath() );
            if( m_children == null )
            {
                m_children = EMPTY_FTP_FILE_ARRAY;
            }
        }

        // Look for the requested child
        // TODO - use hash table
        for( int i = 0; i < m_children.length; i++ )
        {
            FTPFile child = m_children[ i ];
            if( child.getName().equals( name ) )
            {
                // TODO - should be using something else to compare names
                return child;
            }
        }

        return null;
    }

    /**
     * Attaches this file object to its file resource.
     */
    protected void doAttach()
        throws Exception
    {
        // Get the parent folder to find the info for this file
        FtpFileObject parent = (FtpFileObject)getParent();
        m_fileInfo = parent.getChildFile( getName().getBaseName() );
        if( m_fileInfo == null || !m_fileInfo.isDirectory() )
        {
            m_children = EMPTY_FTP_FILE_ARRAY;
        }
    }

    /**
     * Detaches this file object from its file resource.
     */
    protected void doDetach()
    {
        m_fileInfo = null;
        m_children = null;
    }

    /**
     * Called when the children of this file change.
     */
    protected void onChildrenChanged()
    {
        m_children = null;
    }

    /**
     * Determines the type of the file, returns null if the file does not
     * exist.
     */
    protected FileType doGetType()
        throws Exception
    {
        if( m_fileInfo == null )
        {
            // Does not exist
            return null;
        }
        if( m_fileInfo.isDirectory() )
        {
            return FileType.FOLDER;
        }
        if( m_fileInfo.isFile() )
        {
            return FileType.FILE;
        }

        final String message = REZ.getString( "get-type.error", getName() );
        throw new FileSystemException( message );
    }

    /**
     * Lists the children of the file.
     */
    protected String[] doListChildren()
        throws Exception
    {
        if( m_children == null )
        {
            // List the children of this file
            m_children = m_ftpFs.getClient().listFiles( getName().getPath() );
            if( m_children == null )
            {
                m_children = EMPTY_FTP_FILE_ARRAY;
            }
        }

        String[] children = new String[ m_children.length ];
        for( int i = 0; i < m_children.length; i++ )
        {
            FTPFile child = m_children[ i ];
            children[ i ] = child.getName();
        }

        return children;
    }

    /**
     * Deletes the file.
     */
    protected void doDelete() throws Exception
    {
        if( !m_ftpFs.getClient().deleteFile( getName().getPath() ) )
        {
            final String message = REZ.getString( "delete-file.error", getName() );
            throw new FileSystemException( message );
        }
    }

    /**
     * Creates this file as a folder.
     */
    protected void doCreateFolder()
        throws Exception
    {
        if( !m_ftpFs.getClient().makeDirectory( getName().getPath() ) )
        {
            final String message = REZ.getString( "create-folder.error", getName() );
            throw new FileSystemException( message );
        }
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    protected long doGetContentSize() throws Exception
    {
        return m_fileInfo.getSize();
    }

    /**
     * Creates an input stream to read the file content from.
     */
    protected InputStream doGetInputStream() throws Exception
    {
        return m_ftpFs.getClient().retrieveFileStream( getName().getPath() );
    }

    /**
     * Notification of the input stream being closed.
     */
    protected void doEndInput()
        throws Exception
    {
        if( !m_ftpFs.getClient().completePendingCommand() )
        {
            final String message = REZ.getString( "finish-get.error", getName() );
            throw new FileSystemException( message );
        }
    }

    /**
     * Creates an output stream to write the file content to.
     */
    protected OutputStream doGetOutputStream()
        throws Exception
    {
        return m_ftpFs.getClient().storeFileStream( getName().getPath() );
    }

    /**
     * Notification of the output stream being closed.
     */
    protected void doEndOutput()
        throws Exception
    {
        if( !m_ftpFs.getClient().completePendingCommand() )
        {
            final String message = REZ.getString( "finish-put.error", getName() );
            throw new FileSystemException( message );
        }
    }
}
