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
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Abstract Base class for unpack tasks.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public abstract class Unpack
    extends AbstractTask
{
    private File m_dest;
    private File m_src;

    public void setDest( final File dest )
    {
        m_dest = dest;
    }

    public void setSrc( final File src )
    {
        m_src = src;
    }

    public void execute()
        throws TaskException
    {
        validate();

        final File src = getSrc();
        final File dest = getDest();

        if( src.lastModified() > dest.lastModified() )
        {
            final String message = "Expanding " + src.getAbsolutePath() +
                " to " + dest.getAbsolutePath();
            getContext().info( message );

            extract();
        }
    }

    protected abstract String getDefaultExtension();

    protected abstract InputStream getUnpackingStream( InputStream input )
        throws TaskException, IOException;

    private void extract()
        throws TaskException
    {
        OutputStream output = null;
        InputStream input = null;
        InputStream fileInput = null;
        try
        {
            output = new FileOutputStream( getDest() );
            fileInput = new FileInputStream( getSrc() );
            input = getUnpackingStream( fileInput );
            IOUtil.copy( input, output );
        }
        catch( final IOException ioe )
        {
            final String message = "Problem expanding " + getSrc() +
                ":" + ioe.getMessage();
            throw new TaskException( message, ioe );
        }
        finally
        {
            IOUtil.shutdownStream( fileInput );
            IOUtil.shutdownStream( output );
            IOUtil.shutdownStream( input );
        }
    }

    private File createDestFile()
    {
        final String extension = getDefaultExtension();
        final String sourceName = m_src.getName();
        final int length = sourceName.length();
        final int index = length - extension.length();

        if( null != extension &&
            length > extension.length() &&
            extension.equalsIgnoreCase( sourceName.substring( index ) ) )
        {
            final String child = sourceName.substring( 0, index );
            return new File( m_dest, child );
        }
        else
        {
            return new File( m_dest, sourceName );
        }
    }

    private void validate()
        throws TaskException
    {
        if( null == m_src )
        {
            final String message = "No Src for " + getContext().getName() + " specified";
            throw new TaskException( message );
        }

        if( !m_src.exists() )
        {
            final String message = "Src doesn't exist";
            throw new TaskException( message );
        }

        if( m_src.isDirectory() )
        {
            final String message = "Cannot expand a directory";
            throw new TaskException( message );
        }

        if( null == m_dest )
        {
            m_dest = new File( m_src.getParent() );
        }

        if( m_dest.isDirectory() )
        {
            m_dest = createDestFile();
        }
    }

    protected final File getDest()
    {
        return m_dest;
    }

    protected final File getSrc()
    {
        return m_src;
    }
}
