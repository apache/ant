/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.file;

import java.io.File;
import java.io.IOException;
import org.apache.avalon.excalibur.io.FileUtil;

/**
 * A task used to move files.
 *
 * @ant:task name="move"
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class MoveTask
    extends CopyTask
{
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

        if( destination.exists() )
        {
            FileUtil.forceDelete( destination );
        }
        FileUtil.copyFile( source, destination );

        if( isPreserveLastModified() )
        {
            destination.setLastModified( source.lastModified() );
        }

        FileUtil.forceDelete( source );
    }
}
