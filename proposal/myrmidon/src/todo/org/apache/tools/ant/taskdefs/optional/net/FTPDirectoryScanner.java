/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.net;

import com.oroinc.net.ftp.FTPClient;
import com.oroinc.net.ftp.FTPFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.DirectoryScanner;

class FTPDirectoryScanner
    extends DirectoryScanner
{
    protected final FTPClient m_ftp;

    public FTPDirectoryScanner( final FTPClient ftp )
    {
        super();
        m_ftp = ftp;
    }

    public void scan()
        throws TaskException
    {
        if( getIncludes() == null )
        {
            // No includes supplied, so set it to 'matches all'
            setIncludes( new String[ 1 ] );
            getIncludes()[ 0 ] = "**";
        }
        if( getExcludes() == null )
        {
            setExcludes( new String[ 0 ] );
        }

        setFilesIncluded( new ArrayList() );
        setFilesNotIncluded( new ArrayList() );
        setFilesExcluded( new ArrayList() );
        setDirsIncluded( new ArrayList() );
        setDirsNotIncluded( new ArrayList() );
        setDirsExcluded( new ArrayList() );

        try
        {
            String cwd = m_ftp.printWorkingDirectory();
            scandir( ".", "", true );// always start from the current ftp working dir
            m_ftp.changeWorkingDirectory( cwd );
        }
        catch( IOException e )
        {
            throw new TaskException( "Unable to scan FTP server: ", e );
        }
    }

    protected void scandir( String dir, String vpath, boolean fast )
        throws TaskException
    {
        try
        {
            if( !m_ftp.changeWorkingDirectory( dir ) )
            {
                return;
            }

            FTPFile[] newfiles = m_ftp.listFiles();
            if( newfiles == null )
            {
                m_ftp.changeToParentDirectory();
                return;
            }

            for( int i = 0; i < newfiles.length; i++ )
            {
                FTPFile file = newfiles[ i ];
                if( !file.getName().equals( "." ) && !file.getName().equals( ".." ) )
                {
                    if( file.isDirectory() )
                    {
                        String name = file.getName();
                        if( isIncluded( name ) )
                        {
                            if( !isExcluded( name ) )
                            {
                                getDirsIncluded().add( name );
                                if( fast )
                                {
                                    scandir( name, vpath + name + File.separator, fast );
                                }
                            }
                            else
                            {
                                getDirsExcluded().add( name );
                            }
                        }
                        else
                        {
                            getDirsNotIncluded().add( name );
                            if( fast && couldHoldIncluded( name ) )
                            {
                                scandir( name, vpath + name + File.separator, fast );
                            }
                        }
                        if( !fast )
                        {
                            scandir( name, vpath + name + File.separator, fast );
                        }
                    }
                    else
                    {
                        if( file.isFile() )
                        {
                            String name = vpath + file.getName();
                            if( isIncluded( name ) )
                            {
                                if( !isExcluded( name ) )
                                {
                                    getFilesIncluded().add( name );
                                }
                                else
                                {
                                    getFilesExcluded().add( name );
                                }
                            }
                            else
                            {
                                getFilesNotIncluded().add( name );
                            }
                        }
                    }
                }
            }
            m_ftp.changeToParentDirectory();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error while communicating with FTP server: ", e );
        }
    }
}
