/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
    implements ExecuteStreamHandler, Runnable
{
    private final ChangeLogParser m_parser;
    private BufferedReader m_reader;
    private InputStreamReader m_error;
    private final StringBuffer m_errors = new StringBuffer();

    RedirectingStreamHandler( final ChangeLogParser parser )
    {
        m_parser = parser;
    }

    String getErrors()
    {
        if( 0 == m_errors.length() )
        {
            return null;
        }
        else
        {
            return m_errors.toString();
        }
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
        //Start up a separate thread to consume error
        //stream. Hopefully to avoid blocking of task
        final Thread thread = new Thread( this, "ErrorConsumer" );
        thread.start();

        String line = m_reader.readLine();
        while( null != line )
        {
            m_parser.stdout( line );
            line = m_reader.readLine();
        }
    }

    /**
     * Process the standard error in a different
     * thread to avoid blocking in some situaitons.
     */
    public void run()
    {
        // Read the error stream so that it does not block !
        // We cannot use a BufferedReader as the ready() method is bugged!
        // (see Bug 4329985, which is supposed to be fixed in JDK1.4 :
        //http://developer.java.sun.com/developer/bugParade/bugs/4329985.html)
        try
        {
            while( m_error.ready() )
            {
                final int value = m_error.read();
                if( -1 != value )
                {
                    m_errors.append( (char)value );
                }
            }
        }
        catch( final IOException ioe )
        {
            //ignore --> Means stderror has been shutdown
        }
    }

    /**
     * Stop handling of the streams - will not be restarted.
     */
    public void stop()
    {
    }
}
