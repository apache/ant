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
import org.apache.tools.ant.Task;

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
    private final int m_level;
    private final Task m_task;

    private ByteArrayOutputStream m_buffer = new ByteArrayOutputStream();
    private boolean m_skip;

    /**
     * Creates a new instance of this class.
     *
     * @param task the task for whom to log
     * @param level loglevel used to log data written to this stream.
     */
    public LogOutputStream( final Task task, final int level )
    {
        m_task = task;
        m_level = level;
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
        processLine( m_buffer.toString(), m_level );
        m_buffer.reset();
    }

    /**
     * Logs a line to the log system of ant.
     *
     * @param line the line to log.
     * @param level Description of Parameter
     */
    protected void processLine( String line, int level )
    {
        m_task.log( line, level );
    }
}
