/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.tools.ant.types.FilterSetCollection;

/**
 * A stream that substitutes valus while reading a stream according to
 * a FilterSet specified on construction.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class SubstInputStream
    extends InputStream
{
    private final static int MAX_SPAN = 256;
    private final static byte START_TOKEN = (byte)'@';
    private final static byte END_TOKEN = (byte)'@';

    /**
     * The input stream to be filtered.
     */
    private InputStream m_input;

    /**
     * The filters that will be used to replace contents of the stream.
     */
    private final FilterSetCollection m_filters;

    /**
     * The temporary buffer to use during substitution.
     */
    private byte[] m_buffer = new byte[ MAX_SPAN ];

    /**
     * The number of valid bytes stored in the buffer.
     */
    private int m_index;

    /**
     * Construct a stream to replace values as read in using
     * specified filters.
     */
    public SubstInputStream( final InputStream input,
                             final FilterSetCollection filters )
    {
        m_input = input;
        m_filters = filters;
    }

    public int read()
        throws IOException
    {
        return m_input.read();
    }

    public int read( final byte[] data,
                     final int offset,
                     final int length )
        throws IOException
    {
        return m_input.read( data, offset, length );
    }

    public long skip( final long count )
        throws IOException
    {
        if( count > m_index )
        {
            final long remainder = count - m_index;
            final long result = m_input.skip( remainder );
            m_index = 0;
            return result;
        }
        else
        {
            final int shift = (int)count;
            shiftLeft( shift );
            return count;
        }
    }

    public int available()
        throws IOException
    {
        return m_index + m_input.available();
    }

    public void close()
        throws IOException
    {
        m_input.close();
    }

    public synchronized void reset()
        throws IOException
    {
        throw new IOException( "mark/reset not supported" );
    }

    public boolean markSupported()
    {
        return false;
    }

    private void shiftLeft( final int shift )
    {
        final int remainder = m_index - shift;
        arraycopy( m_buffer, shift, m_buffer, 0, remainder );
        m_index = remainder;
    }

    private void arraycopy( final byte[] src,
                            int srcOffset,
                            final byte[] dest,
                            int destOffset,
                            final int length )
    {
        for( int i = 0; i < length; i++ )
        {
            dest[ destOffset ] = src[ srcOffset ];
            srcOffset++;
            destOffset++;
        }
    }

    /*
    public static void copyFile( final File sourceFile,
                                     final File destFile,
                                     final FilterSetCollection filters )
            throws IOException, TaskException
        {
            BufferedReader in = new BufferedReader( new FileReader( sourceFile ) );
            BufferedWriter out = new BufferedWriter( new FileWriter( destFile ) );

            int length;
            String newline = null;
            String line = in.readLine();
            while( line != null )
            {
                if( line.length() == 0 )
                {
                    out.newLine();
                }
                else
                {
                    newline = filters.replaceTokens( line );
                    out.write( newline );
                    out.newLine();
                }
                line = in.readLine();
            }

            out.close();
            in.close();
        }
        */
}