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
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.taskdefs.archive.Expand;

/**
 * Untar a file. Heavily based on the Expand task.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Unzip
    extends Expand
{
    protected void expandArchive( final File src, final File dir )
        throws IOException, TaskException
    {
        ZipInputStream zis = null;
        try
        {
            // code from WarExpand
            zis = new ZipInputStream( new FileInputStream( src ) );
            ZipEntry ze = null;

            while( ( ze = zis.getNextEntry() ) != null )
            {
                final Date date = new Date( ze.getTime() );
                extractFile( dir,
                             zis,
                             ze.getName(),
                             date,
                             ze.isDirectory() );
            }
        }
        finally
        {
            IOUtil.shutdownStream( zis );
        }
    }
}
