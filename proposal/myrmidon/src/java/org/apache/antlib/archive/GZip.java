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
import java.util.zip.GZIPOutputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * Compresses a file with the GZIP algorithm. Normally used to compress
 * non-compressed archives such as TAR files.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class GZip
    extends Pack
{
    protected OutputStream getPackingStream( final OutputStream output )
        throws TaskException, IOException
    {
        return new GZIPOutputStream( output );
    }
}
