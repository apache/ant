/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Abstract Base class for pack tasks.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class Pack
    extends AbstractTask
{
    private File m_src;
    private File m_zipFile;

    public void setSrc( final File src )
    {
        m_src = src;
    }

    public void setZipfile( final File zipFile )
    {
        m_zipFile = zipFile;
    }

    public void execute()
        throws TaskException
    {
        validate();
        final String message = "Building: " + m_zipFile.getAbsolutePath();
        getContext().info( message );
        pack();
    }

    private void pack()
        throws TaskException
    {
        OutputStream output = null;
        try
        {
            final FileOutputStream fileOutput = new FileOutputStream( getZipFile() );
            output = getPackingStream( fileOutput );
            copy( getSrc(), output );
        }
        catch( final IOException ioe )
        {
            final String message = "Problem creating " + getContext().getName() +
                ":" + ioe.getMessage();
            throw new TaskException( message, ioe );
        }
        finally
        {
            IOUtil.shutdownStream( output );
        }
    }

    protected abstract OutputStream getPackingStream( OutputStream output )
        throws TaskException, IOException;

    protected final void copy( final File file, final OutputStream output )
        throws IOException
    {
        final FileInputStream input = new FileInputStream( file );
        try
        {
            IOUtil.copy( input, output );
        }
        finally
        {
            IOUtil.shutdownStream( input );
        }
    }

    private void validate()
        throws TaskException
    {
        if( null == m_zipFile )
        {
            final String message = "zipfile attribute is required";
            throw new TaskException( message );
        }

        if( null == m_src )
        {
            final String message = "src attribute is required";
            throw new TaskException( message );
        }

        if( m_src.isDirectory() )
        {
            final String message = "Src attribute must not " +
                "represent a directory!";
            throw new TaskException( message );
        }
    }

    protected final File getSrc()
    {
        return m_src;
    }

    protected final File getZipFile()
    {
        return m_zipFile;
    }
}
