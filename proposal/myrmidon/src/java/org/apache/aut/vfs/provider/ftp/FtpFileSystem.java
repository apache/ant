/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.ftp;

import com.oroinc.net.ftp.FTP;
import com.oroinc.net.ftp.FTPClient;
import com.oroinc.net.ftp.FTPReply;
import java.io.IOException;
import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystem;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * An FTP file system.
 *
 * @author Adam Murdoch
 */
class FtpFileSystem
    extends AbstractFileSystem
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( FtpFileSystem.class );

    private FTPClient m_client;

    public FtpFileSystem( final FileName rootName,
                          final String hostname,
                          final String username,
                          final String password )
        throws FileSystemException
    {
        super( rootName );
        try
        {
            m_client = new FTPClient();
            m_client.connect( hostname );

            int reply = m_client.getReplyCode();
            if( !FTPReply.isPositiveCompletion( reply ) )
            {
                final String message = REZ.getString( "connect-rejected.error", hostname );
                throw new FileSystemException( message );
            }

            // Login
            if( !m_client.login( username, password ) )
            {
                final String message = REZ.getString( "login.error", hostname, username );
                throw new FileSystemException( message );
            }

            // Set binary mode
            if( !m_client.setFileType( FTP.BINARY_FILE_TYPE ) )
            {
                final String message = REZ.getString( "set-binary.error", hostname );
                throw new FileSystemException( message );
            }
        }
        catch( Exception exc )
        {
            try
            {
                // Clean up
                if( m_client.isConnected() )
                {
                    m_client.disconnect();
                }
            }
            catch( IOException e )
            {
                // Ignore
            }

            final String message = REZ.getString( "connect.error", hostname );
            throw new FileSystemException( message, exc );
        }

        // TODO - close connection
    }

    /**
     * Returns an FTP client to use.
     */
    public FTPClient getClient()
    {
        return m_client;
    }

    /**
     * Creates a file object.
     */
    protected FileObject createFile( FileName name )
        throws FileSystemException
    {
        return new FtpFileObject( name, this );
    }
}
