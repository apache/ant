/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Logs each line written to this stream to the log system of ant. Tries to be
 * smart about line separators.<br>
 * TODO: This class can be split to implement other line based processing of
 * data written to the stream.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class LogOutputStream extends OutputStream
{
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean skip = false;
    private int level = Project.MSG_INFO;

    private Task task;

    /**
     * Creates a new instance of this class.
     *
     * @param task the task for whom to log
     * @param level loglevel used to log data written to this stream.
     */
    public LogOutputStream( Task task, int level )
    {
        this.task = task;
        this.level = level;
    }

    public int getMessageLevel()
    {
        return level;
    }

    /**
     * Writes all remaining
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        if( buffer.size() > 0 )
            processBuffer();
        super.close();
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
        final byte c = (byte)cc;
        if( ( c == '\n' ) || ( c == '\r' ) )
        {
            if( !skip )
                processBuffer();
        }
        else
            buffer.write( cc );
        skip = ( c == '\r' );
    }

    /**
     * Converts the buffer to a string and sends it to <code>processLine</code>
     */
    protected void processBuffer()
    {
        processLine( buffer.toString() );
        buffer.reset();
    }

    /**
     * Logs a line to the log system of ant.
     *
     * @param line the line to log.
     */
    protected void processLine( String line )
    {
        processLine( line, level );
    }

    /**
     * Logs a line to the log system of ant.
     *
     * @param line the line to log.
     * @param level Description of Parameter
     */
    protected void processLine( String line, int level )
    {
        task.log( line, level );
    }
}
