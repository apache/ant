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
import java.util.zip.GZIPInputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * Expands a file that has been compressed with the GZIP algorithm. Normally
 * used to compress non-compressed archives such as TAR files.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @ant.task name="gunzip"
 */
public class GUnzip
    extends Unpack
{
    private static final String DEFAULT_EXTENSION = ".gz";

    protected String getDefaultExtension()
    {
        return DEFAULT_EXTENSION;
    }

    protected InputStream getUnpackingStream( InputStream input )
        throws TaskException, IOException
    {
        return new GZIPInputStream( input );
    }
}
