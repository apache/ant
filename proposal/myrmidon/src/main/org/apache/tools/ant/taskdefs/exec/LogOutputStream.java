/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.avalon.framework.logger.Logger;

/**
 * Logs each line written to this stream to the log system of ant. Tries to be
 * smart about line separators.<br>
 * TODO: This class can be split to implement other line based processing of
 * data written to the stream.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class LogOutputStream
    extends OutputStream
{
    private final boolean m_isError;
    private final Logger m_logger;

    private ByteArrayOutputStream m_buffer = new ByteArrayOutputStream();
    private boolean m_skip;

    public LogOutputStream( final Logger logger, final boolean isError )
    {
        m_logger = logger;
        m_isError = isError;
    }

    protected final Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Writes all remaining
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        if( m_buffer.size() > 0 )
        {
            processBuffer();
        }
        super.close();
    }

    /**
     * Write the data to the buffer and flush the buffer, if a line separator is
     * detected.
     *
     * @param cc data to log (byte).
     * @exception IOException Description of Exception
     */
    public void write( final int ch )
        throws IOException
    {
        if( ( ch == '\n' ) || ( ch == '\r' ) )
        {
            if( !m_skip )
            {
                processBuffer();
            }
        }
        else
        {
            m_buffer.write( (byte)ch );
        }

        m_skip = ( ch == '\r' );
    }

    /**
     * Converts the buffer to a string and sends it to <code>processLine</code>
     */
    private void processBuffer()
    {
        processLine( m_buffer.toString() );
        m_buffer.reset();
    }

    /**
     * Logs a line to the log system of ant.
     *
     * @param line the line to log.
     * @param level Description of Parameter
     */
    protected void processLine( final String line )
    {
        if( m_isError )
        {
            getLogger().warn( line );
        }
        else
        {
            getLogger().info( line );
        }
    }

    public boolean isError()
    {
        return m_isError;
    }
}
