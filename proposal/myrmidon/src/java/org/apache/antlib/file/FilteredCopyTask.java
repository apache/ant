/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.filters.LineFilterSet;

/**
 * A task used to copy files and simultaneously apply a
 * filter on said files.
 *
 * @ant:task name="filtered-copy"
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class FilteredCopyTask
    extends CopyTask
{
    private LineFilterSet m_filterSetCollection = new LineFilterSet();
    private String m_encoding = "US-ASCII";

    public void addFilterset( final LineFilterSet filter )
    {
        m_filterSetCollection.add( filter );
    }

    public void setEncoding( final String encoding )
    {
        m_encoding = encoding;
    }

    /**
     * Utility method to perform operation to transform a single source file
     * to a destination.
     */
    protected void doOperation( final String sourceFilename,
                                final String destinationFilename )
        throws IOException
    {
        final File source = new File( sourceFilename );
        final File destination = new File( destinationFilename );

        InputStream inputStream = null;
        OutputStream outputStream = null;
        BufferedReader input = null;
        BufferedWriter output = null;
        try
        {
            inputStream = new FileInputStream( source );
            outputStream = new FileOutputStream( destination );

            final Reader fileReader = new InputStreamReader( inputStream, m_encoding );
            final Writer fileWriter = new OutputStreamWriter( outputStream, m_encoding );
            input = new BufferedReader( fileReader );
            output = new BufferedWriter( fileWriter );

            process( input, output );
        }
        catch( final UnsupportedEncodingException uee )
        {
            throw new IOException( uee.toString() );
        }
        finally
        {
            IOUtil.shutdownReader( input );
            IOUtil.shutdownStream( inputStream );
            IOUtil.shutdownWriter( output );
            IOUtil.shutdownStream( outputStream );
        }

        if( isPreserveLastModified() )
        {
            destination.setLastModified( source.lastModified() );
        }
    }

    private void process( final BufferedReader input,
                          final BufferedWriter output )
        throws IOException
    {
        String newline = null;
        String line = input.readLine();
        while( null != line )
        {
            if( line.length() == 0 )
            {
                output.newLine();
            }
            else
            {
                newline = replaceTokens( line );
                output.write( newline );
                output.newLine();
            }
            line = input.readLine();
        }
    }

    private String replaceTokens( final String line )
        throws IOException
    {
        try
        {
            final StringBuffer buffer = new StringBuffer( line );
            m_filterSetCollection.filterLine( buffer, getContext() );
            return buffer.toString();
        }
        catch( final TaskException te )
        {
            throw new IOException( te.getMessage() );
        }
    }
}
