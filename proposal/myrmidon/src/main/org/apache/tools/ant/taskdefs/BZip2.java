/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Pack;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 * Compresses a file with the BZip2 algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */

public class BZip2 extends Pack
{
    protected void pack()
    {
        CBZip2OutputStream zOut = null;
        try
        {
            BufferedOutputStream bos =
                new BufferedOutputStream( new FileOutputStream( zipFile ) );
            bos.write( 'B' );
            bos.write( 'Z' );
            zOut = new CBZip2OutputStream( bos );
            zipFile( source, zOut );
        }
        catch( IOException ioe )
        {
            String msg = "Problem creating bzip2 " + ioe.getMessage();
            throw new BuildException( msg, ioe, location );
        }
        finally
        {
            if( zOut != null )
            {
                try
                {
                    // close up
                    zOut.close();
                }
                catch( IOException e )
                {}
            }
        }
    }
}
