/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

/**
 * Abstract Base class for pack tasks.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */

public abstract class Pack extends Task
{
    protected File source;

    protected File zipFile;

    public void setSrc( File src )
    {
        source = src;
    }

    public void setZipfile( File zipFile )
    {
        this.zipFile = zipFile;
    }

    public void execute()
        throws TaskException
    {
        validate();
        log( "Building: " + zipFile.getAbsolutePath() );
        pack();
    }

    protected abstract void pack();

    protected void zipFile( File file, OutputStream zOut )
        throws IOException
    {
        FileInputStream fIn = new FileInputStream( file );
        try
        {
            zipFile( fIn, zOut );
        }
        finally
        {
            fIn.close();
        }
    }

    private void validate()
    {
        if( zipFile == null )
        {
            throw new TaskException( "zipfile attribute is required" );
        }

        if( source == null )
        {
            throw new TaskException( "src attribute is required" );
        }

        if( source.isDirectory() )
        {
            throw new TaskException( "Src attribute must not " +
                                     "represent a directory!" );
        }
    }

    private void zipFile( InputStream in, OutputStream zOut )
        throws IOException
    {
        byte[] buffer = new byte[ 8 * 1024 ];
        int count = 0;
        do
        {
            zOut.write( buffer, 0, count );
            count = in.read( buffer, 0, buffer.length );
        } while( count != -1 );
    }
}
