/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * Expands a file that has been compressed with the GZIP algorithm. Normally
 * used to compress non-compressed archives such as TAR files.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */

public class GUnzip extends Unpack
{

    private final static String DEFAULT_EXTENSION = ".gz";

    protected String getDefaultExtension()
    {
        return DEFAULT_EXTENSION;
    }

    protected void extract()
        throws TaskException
    {
        if( source.lastModified() > dest.lastModified() )
        {
            getLogger().info( "Expanding " + source.getAbsolutePath() + " to "
                              + dest.getAbsolutePath() );

            FileOutputStream out = null;
            GZIPInputStream zIn = null;
            FileInputStream fis = null;
            try
            {
                out = new FileOutputStream( dest );
                fis = new FileInputStream( source );
                zIn = new GZIPInputStream( fis );
                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    out.write( buffer, 0, count );
                    count = zIn.read( buffer, 0, buffer.length );
                } while( count != -1 );
            }
            catch( IOException ioe )
            {
                String msg = "Problem expanding gzip " + ioe.getMessage();
                throw new TaskException( msg, ioe );
            }
            finally
            {
                if( fis != null )
                {
                    try
                    {
                        fis.close();
                    }
                    catch( IOException ioex )
                    {
                    }
                }
                if( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch( IOException ioex )
                    {
                    }
                }
                if( zIn != null )
                {
                    try
                    {
                        zIn.close();
                    }
                    catch( IOException ioex )
                    {
                    }
                }
            }
        }
    }
}
