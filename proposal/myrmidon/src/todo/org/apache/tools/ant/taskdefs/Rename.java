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
 * Renames a file.
 *
 * @author haas@softwired.ch
 * @deprecated The rename task is deprecated. Use move instead.
 */
public class Rename extends Task
{
    private boolean replace = true;
    private File dest;

    private File src;

    /**
     * Sets the new name of the file.
     *
     * @param dest the new name of the file.
     */
    public void setDest( File dest )
    {
        this.dest = dest;
    }

    /**
     * Sets wheter an existing file should be replaced.
     *
     * @param replace <code>on</code>, if an existing file should be replaced.
     */
    public void setReplace( String replace )
    {
        this.replace = project.toBoolean( replace );
    }


    /**
     * Sets the file to be renamed.
     *
     * @param src the file to rename
     */
    public void setSrc( File src )
    {
        this.src = src;
    }


    /**
     * Renames the file <code>src</code> to <code>dest</code>
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {
        log( "DEPRECATED - The rename task is deprecated.  Use move instead." );

        if( dest == null )
        {
            throw new BuildException( "dest attribute is required", location );
        }

        if( src == null )
        {
            throw new BuildException( "src attribute is required", location );
        }

        if( replace && dest.exists() )
        {
            if( !dest.delete() )
            {
                throw new BuildException( "Unable to remove existing file " +
                    dest );
            }
        }
        if( !src.renameTo( dest ) )
        {
            throw new BuildException( "Unable to rename " + src + " to " +
                dest );
        }
    }
}
