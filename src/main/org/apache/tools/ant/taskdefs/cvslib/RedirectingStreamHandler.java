/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.cvslib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;

/**
 * A dummy stream handler that just passes stuff to the parser.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class RedirectingStreamHandler
    implements ExecuteStreamHandler
{
    private final ChangeLogParser m_parser;
    private BufferedReader m_reader;
    private InputStreamReader m_error;

    public RedirectingStreamHandler( final ChangeLogParser parser )
    {
        m_parser = parser;
    }

    /**
     * Install a handler for the input stream of the subprocess.
     *
     * @param os output stream to write to the standard input stream of the
     *           subprocess
     */
    public void setProcessInputStream( OutputStream os ) throws IOException
    {
        //ignore
    }

    /**
     * Install a handler for the error stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     */
    public void setProcessErrorStream( InputStream is ) throws IOException
    {
        m_error = new InputStreamReader( is );
    }

    /**
     * Install a handler for the output stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     */
    public void setProcessOutputStream( InputStream is ) throws IOException
    {
        m_reader = new BufferedReader( new InputStreamReader( is ) );
    }

    /**
     * Start handling of the streams.
     */
    public void start() throws IOException
    {
        String line = m_reader.readLine();
        while( null != line )
        {
            m_parser.stdout( line );
            line = m_reader.readLine();
        }

        // Read the error stream so that it does not block !
        // We cannot use a BufferedReader as the ready() method is bugged!
        // (see Bug 4329985, which is supposed to be fixed in JDK1.4 :
        //http://developer.java.sun.com/developer/bugParade/bugs/4329985.html)
        while( m_error.ready() )
        {
            m_error.read();
        }
    }

    /**
     * Stop handling of the streams - will not be restarted.
     */
    public void stop()
    {
    }
}
