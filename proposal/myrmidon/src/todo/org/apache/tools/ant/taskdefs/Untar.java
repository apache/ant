/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.tar.TarEntry;
import org.apache.aut.tar.TarInputStream;

/**
 * Untar a file. Heavily based on the Expand task.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Untar extends Expand
{

    protected void expandFile( File srcF, File dir )
        throws TaskException
    {
        TarInputStream tis = null;
        try
        {
            getLogger().info( "Expanding: " + srcF + " into " + dir );

            tis = new TarInputStream( new FileInputStream( srcF ) );
            TarEntry te = null;

            while( ( te = tis.getNextEntry() ) != null )
            {
                extractFile( srcF, dir, tis,
                             te.getName(),
                             te.getModTime(), te.isDirectory() );
            }
            getLogger().debug( "expand complete" );

        }
        catch( IOException ioe )
        {
            throw new TaskException( "Error while expanding " + srcF.getPath(), ioe );
        }
        finally
        {
            if( tis != null )
            {
                try
                {
                    tis.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }
}
