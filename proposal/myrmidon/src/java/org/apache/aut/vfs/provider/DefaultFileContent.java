/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Date;
import org.apache.aut.vfs.FileContent;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.i18n.ResourceManager;

/**
 * The content of a file.
 *
 * @author Adam Murdoch
 */
public class DefaultFileContent implements FileContent
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( DefaultFileContent.class );

    private AbstractFileObject m_file;
    private int _state = STATE_NONE;
    private FileContentInputStream m_instr;
    private FileContentOutputStream m_outstr;

    private static final int STATE_NONE = 0;
    private static final int STATE_READING = 1;
    private static final int STATE_WRITING = 2;

    public DefaultFileContent( AbstractFileObject file )
    {
        m_file = file;
    }

    /**
     * Returns the file which this is the content of.
     */
    public FileObject getFile()
    {
        return m_file;
    }

    /**
     * Returns the size of the content (in bytes).
     */
    public long getSize() throws FileSystemException
    {
        // Do some checking
        if( !m_file.exists() )
        {
            final String message = REZ.getString( "get-size-no-exist.error", m_file );
            throw new FileSystemException( message );
        }
        if( _state == STATE_WRITING )
        {
            final String message = REZ.getString( "get-size-write.error", m_file );
            throw new FileSystemException( message );
        }

        try
        {
            // Get the size
            return m_file.doGetContentSize();
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "get-size.error", m_file );
            throw new FileSystemException( message, exc );
        }
    }

    /**
     * Returns the last-modified timestamp.
     */
    public long getLastModifiedTime() throws FileSystemException
    {
        // TODO - implement this
        throw new FileSystemException( "Not implemented." );
    }

    /**
     * Sets the last-modified timestamp.
     */
    public void setLastModifiedTime( long modTime ) throws FileSystemException
    {
        // TODO - implement this
        throw new FileSystemException( "Not implemented." );
    }

    /**
     * Gets the value of an attribute.
     */
    public Object getAttribute( String attrName ) throws FileSystemException
    {
        // TODO - implement this
        throw new FileSystemException( "Not implemented." );
    }

    /**
     * Sets the value of an attribute.
     */
    public void setAttribute( String attrName, Object value ) throws FileSystemException
    {
        // TODO - implement this
        throw new FileSystemException( "Not implemented." );
    }

    /**
     * Returns an input stream for reading the content.
     */
    public InputStream getInputStream() throws FileSystemException
    {
        if( !m_file.exists() )
        {
            final String message = REZ.getString( "read-no-exist.error", m_file );
            throw new FileSystemException( message );
        }
        if( _state != STATE_NONE )
        {
            final String message = REZ.getString( "read-in-use.error", m_file );
            throw new FileSystemException( message );
        }

        // Get the raw input stream
        InputStream instr = null;
        try
        {
            instr = m_file.doGetInputStream();
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "read.error", m_file );
            throw new FileSystemException( message, exc );
        }

        // TODO - reuse
        m_instr = new FileContentInputStream( instr );
        _state = STATE_READING;
        return m_instr;
    }

    /**
     * Returns an output stream for writing the content.
     */
    public OutputStream getOutputStream() throws FileSystemException
    {
        if( _state != STATE_NONE )
        {
            final String message = REZ.getString( "write-in-use.error", m_file );
            throw new FileSystemException( message );
        }

        // Get the raw output stream
        OutputStream outstr = m_file.getOutputStream();

        // Create wrapper
        // TODO - reuse
        m_outstr = new FileContentOutputStream( outstr );
        _state = STATE_WRITING;
        return m_outstr;
    }

    /**
     * Closes all resources used by the content, including all streams, readers
     * and writers.
     */
    public void close() throws FileSystemException
    {

        try
        {
            // Close the input stream
            if( m_instr != null )
            {
                try
                {
                    m_instr.close();
                }
                catch( IOException ioe )
                {
                    final String message = REZ.getString( "close-instr.error" );
                    throw new FileSystemException( message, ioe );
                }
            }

            // Close the output stream
            if( m_outstr != null )
            {
                try
                {
                    m_outstr.close();
                }
                catch( IOException ioe )
                {
                    final String message = REZ.getString( "close-outstr.error" );
                    throw new FileSystemException( message, ioe );
                }
            }
        }
        finally
        {
            _state = STATE_NONE;
        }
    }

    /**
     * Handles the end of input stream.
     */
    private void endInput() throws Exception
    {
        m_instr = null;
        _state = STATE_NONE;
        m_file.doEndInput();
    }

    /**
     * Handles the end of output stream.
     */
    private void endOutput() throws Exception
    {
        m_outstr = null;
        _state = STATE_NONE;
        m_file.endOutput();
    }

    /**
     * An input stream for reading content.  Provides buffering, and
     * end-of-stream monitoring.
     */
    private final class FileContentInputStream extends BufferedInputStream
    {
        boolean _finished;

        FileContentInputStream( InputStream instr )
        {
            super( instr );
        }

        /**
         * Reads a character.
         */
        public int read() throws IOException
        {
            if( _finished )
            {
                return -1;
            }

            int ch = super.read();
            if( ch != -1 )
            {
                return ch;
            }

            // End-of-stream
            close();
            return -1;
        }

        /**
         * Reads bytes from this input stream.error occurs.
         */
        public int read( byte b[], int off, int len )
            throws IOException
        {
            if( _finished )
            {
                return -1;
            }

            int nread = super.read( b, off, len );
            if( nread != -1 )
            {
                return nread;
            }

            // End-of-stream
            close();
            return -1;
        }

        /**
         * Closes this input stream.
         */
        public void close() throws IOException
        {
            if( _finished )
            {
                return;
            }

            // Close the stream
            IOException exc = null;
            try
            {
                super.close();
            }
            catch( IOException e )
            {
                exc = e;
            }

            // Notify the file object
            try
            {
                endInput();
            }
            catch( Exception e )
            {
                exc = new IOException( e.getMessage() );
            }

            _finished = true;

            if( exc != null )
            {
                throw exc;
            }
        }
    }

    /**
     * An output stream for writing content.
     */
    private final class FileContentOutputStream extends BufferedOutputStream
    {
        FileContentOutputStream( OutputStream outstr )
        {
            super( outstr );
        }

        /**
         * Closes this output stream.
         */
        public void close() throws IOException
        {
            IOException exc = null;

            // Close the output stream
            try
            {
                super.close();
            }
            catch( IOException e )
            {
                exc = e;
            }

            // Notify of end of output
            try
            {
                endOutput();
            }
            catch( Exception e )
            {
                exc = new IOException( e.getMessage() );
            }

            if( exc != null )
            {
                throw exc;
            }
        }
    }

}
