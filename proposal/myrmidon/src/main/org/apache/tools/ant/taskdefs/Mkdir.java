/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
 * Creates a given directory.
 *
 * @author duncan@x180.com
 */

public class Mkdir extends Task
{

    private File dir;

    public void setDir( File dir )
    {
        this.dir = dir;
    }

    public void execute()
        throws BuildException
    {
        if( dir == null )
        {
            throw new BuildException( "dir attribute is required", location );
        }

        if( dir.isFile() )
        {
            throw new BuildException( "Unable to create directory as a file already exists with that name: " + dir.getAbsolutePath() );
        }

        if( !dir.exists() )
        {
            boolean result = dir.mkdirs();
            if( result == false )
            {
                String msg = "Directory " + dir.getAbsolutePath() + " creation was not " +
                    "successful for an unknown reason";
                throw new BuildException( msg, location );
            }
            log( "Created dir: " + dir.getAbsolutePath() );
        }
    }
}
