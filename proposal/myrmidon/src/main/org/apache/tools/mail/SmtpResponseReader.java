/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A wrapper around the raw input from the SMTP server that assembles multi line
 * responses into a single String. <p>
 *
 * The same rules used here would apply to FTP and other Telnet based protocols
 * as well.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class SmtpResponseReader
{

    protected BufferedReader reader = null;
    private StringBuffer result = new StringBuffer();

    /**
     * Wrap this input stream.
     *
     * @param in Description of Parameter
     */
    public SmtpResponseReader( InputStream in )
    {
        reader = new BufferedReader( new InputStreamReader( in ) );
    }

    /**
     * Read until the server indicates that the response is complete.
     *
     * @return Responsecode (3 digits) + Blank + Text from all response line
     *      concatenated (with blanks replacing the \r\n sequences).
     * @exception IOException Description of Exception
     */
    public String getResponse()
        throws IOException
    {
        result.setLength( 0 );
        String line = reader.readLine();
        if( line != null && line.length() >= 3 )
        {
            result.append( line.substring( 0, 3 ) );
            result.append( " " );
        }

        while( line != null )
        {
            append( line );
            if( !hasMoreLines( line ) )
            {
                break;
            }
            line = reader.readLine();
        }
        return result.toString().trim();
    }

    /**
     * Closes the underlying stream.
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        reader.close();
    }

    /**
     * Should we expect more input?
     *
     * @param line Description of Parameter
     * @return Description of the Returned Value
     */
    protected boolean hasMoreLines( String line )
    {
        return line.length() > 3 && line.charAt( 3 ) == '-';
    }

    /**
     * Append the text from this line of the resonse.
     *
     * @param line Description of Parameter
     */
    private void append( String line )
    {
        if( line.length() > 4 )
        {
            result.append( line.substring( 4 ) );
            result.append( " " );
        }
    }
}
