/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Pack;

/**
 * Compresses a file with the GZIP algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */

public class GZip extends Pack
{
    protected void pack()
    {
        GZIPOutputStream zOut = null;
        try
        {
            zOut = new GZIPOutputStream( new FileOutputStream( zipFile ) );
            zipFile( source, zOut );
        }
        catch( IOException ioe )
        {
            String msg = "Problem creating gzip " + ioe.getMessage();
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
