/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.aut.tar.TarEntry;
import org.apache.aut.tar.TarInputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.taskdefs.archive.Expand;

/**
 * Untar a file. Heavily based on the Expand task.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Untar
    extends Expand
{
    protected void expandArchive( final File src, final File dir )
        throws IOException, TaskException
    {
        TarInputStream input = null;
        FileInputStream fileInput = null;
        try
        {
            fileInput = new FileInputStream( src );
            input = new TarInputStream( fileInput );

            TarEntry entry = null;
            while( ( entry = input.getNextEntry() ) != null )
            {
                extractFile( dir,
                             input,
                             entry.getName(),
                             entry.getModTime(),
                             entry.isDirectory() );
            }
        }
        finally
        {
            IOUtil.shutdownStream( fileInput );
            IOUtil.shutdownStream( input );
        }
    }
}
