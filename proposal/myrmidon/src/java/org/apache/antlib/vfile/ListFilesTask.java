/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * A debug task, which prints out the files in a file list.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:task name="v-list-files"
 */
public class ListFilesTask
    extends AbstractTask
{
    private final DefaultFileList m_files = new DefaultFileList();

    public void add( final FileList files )
    {
        m_files.add( files );
    }

    /**
     * Execute task.
     *
     * @exception TaskException if an error occurs
     */
    public void execute()
        throws TaskException
    {
        final FileObject[] files = m_files.listFiles( getContext() );
        for( int i = 0; i < files.length; i++ )
        {
            FileObject file = files[i ];
            getLogger().info( file.toString() );
        }
    }
}
