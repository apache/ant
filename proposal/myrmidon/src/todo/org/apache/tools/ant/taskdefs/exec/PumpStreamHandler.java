/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * Copies standard output and error of subprocesses to standard output and error
 * of the parent process. TODO: standard input of the subprocess is not
 * implemented.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class PumpStreamHandler
    implements ExecuteStreamHandler
{
    private Thread m_errorThread;
    private Thread m_inputThread;

    private OutputStream m_output;
    private OutputStream m_error;

    public PumpStreamHandler( final OutputStream output,
                              final OutputStream error )
    {
        m_output = output;
        m_error = error;
    }

    public PumpStreamHandler( OutputStream outAndErr )
    {
        this( outAndErr, outAndErr );
    }

    public PumpStreamHandler()
    {
        this( System.out, System.err );
    }

    public void setProcessErrorStream( final InputStream error )
    {
        createProcessErrorPump( error, m_error );
    }

    public void setProcessInputStream( final OutputStream standardInput )
    {
    }

    public void setProcessOutputStream( final InputStream standardOutput )
    {
        createProcessOutputPump( standardOutput, m_output );
    }

    public void start()
    {
        m_inputThread.start();
        m_errorThread.start();
    }

    public void stop()
        throws TaskException
    {
        try
        {
            m_inputThread.join();
        }
        catch( InterruptedException e )
        {
        }
        try
        {
            m_errorThread.join();
        }
        catch( InterruptedException e )
        {
        }
        try
        {
            m_error.flush();
        }
        catch( IOException e )
        {
        }
        try
        {
            m_output.flush();
        }
        catch( IOException e )
        {
        }
    }

    protected OutputStream getErr()
    {
        return m_error;
    }

    protected OutputStream getOut()
    {
        return m_output;
    }

    protected void createProcessErrorPump( InputStream is, OutputStream os )
    {
        m_errorThread = createPump( is, os );
    }

    protected void createProcessOutputPump( InputStream is, OutputStream os )
    {
        m_inputThread = createPump( is, os );
    }

    /**
     * Creates a stream pumper to copy the given input stream to the given
     * output stream.
     *
     * @param is Description of Parameter
     * @param os Description of Parameter
     * @return Description of the Returned Value
     */
    protected Thread createPump( final InputStream input,
                                 final OutputStream output )
    {
        final Thread result = new Thread( new StreamPumper( input, output ) );
        result.setDaemon( true );
        return result;
    }
}
