/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.archive;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.aut.bzip2.CBZip2OutputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * Compresses a file with the BZip2 algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="bzip2"
 */
public class BZip2
    extends Pack
{
    private final static byte[] HEADER = new byte[]{(byte)'B', (byte)'Z'};

    protected OutputStream getPackingStream( OutputStream output )
        throws TaskException, IOException
    {
        output.write( HEADER );
        return new CBZip2OutputStream( output );
    }
}
