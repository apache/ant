/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.file;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.FileList;
import org.apache.myrmidon.framework.file.Path;

/**
 * A diagnostic task that lists the contents of a path.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:task name="list-path"
 */
public class ListPathTask
    extends AbstractTask
{
    private final Path m_path = new Path();

    /**
     * Adds a nested path.
     */
    public void add( final FileList list )
    {
        m_path.add( list );
    }

    /**
     * Executes the task.
     */
    public void execute()
        throws TaskException
    {
        final String[] files = m_path.listFiles( getContext() );
        for( int i = 0; i < files.length; i++ )
        {
            final String file = files[ i ];
            getContext().info( file );
        }
    }
}
