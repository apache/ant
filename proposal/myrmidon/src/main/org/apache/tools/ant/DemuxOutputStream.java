/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;


/**
 * Logs content written by a thread and forwards the buffers onto the project
 * object which will forward the content to the appropriate task
 *
 * @author Conor MacNeill
 */
public class DemuxOutputStream extends OutputStream
{

    private final static int MAX_SIZE = 1024;

    private Hashtable buffers = new Hashtable();
//    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean skip = false;
    private boolean isErrorStream;
    private Project project;

    /**
     * Creates a new instance of this class.
     *
     * @param project Description of Parameter
     * @param isErrorStream Description of Parameter
     */
    public DemuxOutputStream( Project project, boolean isErrorStream )
    {
        this.project = project;
        this.isErrorStream = isErrorStream;
    }

    /**
     * Writes all remaining
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        flush();
    }

    /**
     * Writes all remaining
     *
     * @exception IOException Description of Exception
     */
    public void flush()
        throws IOException
    {
        if( getBuffer().size() > 0 )
        {
            processBuffer();
        }
    }

    /**
     * Write the data to the buffer and flush the buffer, if a line separator is
     * detected.
     *
     * @param cc data to log (byte).
     * @exception IOException Description of Exception
     */
    public void write( int cc )
        throws IOException
    {
        final byte c = ( byte )cc;
        if( ( c == '\n' ) || ( c == '\r' ) )
        {
            if( !skip )
            {
                processBuffer();
            }
        }
        else
        {
            ByteArrayOutputStream buffer = getBuffer();
            buffer.write( cc );
            if( buffer.size() > MAX_SIZE )
            {
                processBuffer();
            }
        }
        skip = ( c == '\r' );
    }


    /**
     * Converts the buffer to a string and sends it to <code>processLine</code>
     */
    protected void processBuffer()
    {
        String output = getBuffer().toString();
        project.demuxOutput( output, isErrorStream );
        resetBuffer();
    }

    private ByteArrayOutputStream getBuffer()
    {
        Thread current = Thread.currentThread();
        ByteArrayOutputStream buffer = ( ByteArrayOutputStream )buffers.get( current );
        if( buffer == null )
        {
            buffer = new ByteArrayOutputStream();
            buffers.put( current, buffer );
        }
        return buffer;
    }

    private void resetBuffer()
    {
        Thread current = Thread.currentThread();
        buffers.remove( current );
    }
}
