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
 * Abstract Base class for unpack tasks.
 *
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */

public abstract class Unpack extends Task
{
    protected File dest;

    protected File source;

    public void setDest( String dest )
    {
        this.dest = resolveFile( dest );
    }

    public void setSrc( String src )
    {
        source = resolveFile( src );
    }

    public void execute()
        throws BuildException
    {
        validate();
        extract();
    }

    protected abstract String getDefaultExtension();

    protected abstract void extract();

    private void createDestFile( String defaultExtension )
    {
        String sourceName = source.getName();
        int len = sourceName.length();
        if( defaultExtension != null
             && len > defaultExtension.length()
             && defaultExtension.equalsIgnoreCase( sourceName.substring( len - defaultExtension.length() ) ) )
        {
            dest = new File( dest, sourceName.substring( 0,
                len - defaultExtension.length() ) );
        }
        else
        {
            dest = new File( dest, sourceName );
        }
    }

    private void validate()
        throws BuildException
    {
        if( source == null )
        {
            throw new BuildException( "No Src for gunzip specified" );
        }

        if( !source.exists() )
        {
            throw new BuildException( "Src doesn't exist" );
        }

        if( source.isDirectory() )
        {
            throw new BuildException( "Cannot expand a directory" );
        }

        if( dest == null )
        {
            dest = new File( source.getParent() );
        }

        if( dest.isDirectory() )
        {
            String defaultExtension = getDefaultExtension();
            createDestFile( defaultExtension );
        }
    }
}
