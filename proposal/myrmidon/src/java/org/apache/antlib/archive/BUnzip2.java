/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.archive;

import java.io.IOException;
import java.io.InputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.bzip2.CBZip2InputStream;

/**
 * Expands a file that has been compressed with the BZIP2 algorithm. Normally
 * used to compress non-compressed archives such as TAR files.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class BUnzip2
    extends Unpack
{
    private final static String DEFAULT_EXTENSION = ".bz2";

    protected String getDefaultExtension()
    {
        return DEFAULT_EXTENSION;
    }

    protected InputStream getUnpackingStream( final InputStream input )
        throws TaskException, IOException
    {
        final int b1 = input.read();
        final int b2 = input.read();
        if( b1 != 'B' || b2 != 'Z' )
        {
            final String message = "Invalid bz2 file.";
            throw new TaskException( message );
        }

        return new CBZip2InputStream( input );
    }
}
