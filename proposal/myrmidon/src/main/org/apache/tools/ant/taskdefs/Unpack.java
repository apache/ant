/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
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
        throws TaskException
    {
        this.dest = resolveFile( dest );
    }

    public void setSrc( String src )
        throws TaskException
    {
        source = resolveFile( src );
    }

    public void execute()
        throws TaskException
    {
        validate();
        extract();
    }

    protected abstract String getDefaultExtension();

    protected abstract void extract()
        throws TaskException;

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
        throws TaskException
    {
        if( source == null )
        {
            throw new TaskException( "No Src for gunzip specified" );
        }

        if( !source.exists() )
        {
            throw new TaskException( "Src doesn't exist" );
        }

        if( source.isDirectory() )
        {
            throw new TaskException( "Cannot expand a directory" );
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
