/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author duncan@x180.com
 * @deprecated The deltree task is deprecated. Use delete instead.
 */

public class Deltree extends Task
{

    private File dir;

    public void setDir( File dir )
    {
        this.dir = dir;
    }

    public void execute()
        throws BuildException
    {
        log( "DEPRECATED - The deltree task is deprecated.  Use delete instead." );

        if( dir == null )
        {
            throw new BuildException( "dir attribute must be set!", location );
        }

        if( dir.exists() )
        {
            if( !dir.isDirectory() )
            {
                if( !dir.delete() )
                {
                    throw new BuildException( "Unable to delete directory "
                         + dir.getAbsolutePath(),
                        location );
                }
                return;
                // String msg = "Given dir: " + dir.getAbsolutePath() +
                // " is not a dir";
                // throw new BuildException(msg);
            }

            log( "Deleting: " + dir.getAbsolutePath() );

            try
            {
                removeDir( dir );
            }
            catch( IOException ioe )
            {
                String msg = "Unable to delete " + dir.getAbsolutePath();
                throw new BuildException( msg, location );
            }
        }
    }

    private void removeDir( File dir )
        throws IOException
    {

        // check to make sure that the given dir isn't a symlink
        // the comparison of absolute path and canonical path
        // catches this

        //        if (dir.getCanonicalPath().equals(dir.getAbsolutePath())) {
        // (costin) It will not work if /home/costin is symlink to /da0/home/costin ( taz
        // for example )
        String[] list = dir.list();
        for( int i = 0; i < list.length; i++ )
        {
            String s = list[i];
            File f = new File( dir, s );
            if( f.isDirectory() )
            {
                removeDir( f );
            }
            else
            {
                if( !f.delete() )
                {
                    throw new BuildException( "Unable to delete file " + f.getAbsolutePath() );
                }
            }
        }
        if( !dir.delete() )
        {
            throw new BuildException( "Unable to delete directory " + dir.getAbsolutePath() );
        }
    }
}

