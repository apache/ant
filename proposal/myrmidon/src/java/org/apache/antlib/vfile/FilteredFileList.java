/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
import org.apache.antlib.vfile.selectors.AndFileSelector;
import org.apache.antlib.vfile.selectors.FileSelector;
import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file-list which filters another.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.data-type name="filtered-path"
 * @ant.type type="v-path" name="filtered-path"
 */
public class FilteredFileList
    implements FileList
{
    private DefaultFileList m_fileList = new DefaultFileList();
    private FileSelector m_selector;

    /**
     * Sets the selector to use to filter with.
     */
    public void setFilter( final AndFileSelector selector )
    {
        m_selector = selector;
    }

    /**
     * Sets the filelist to filter.
     */
    public void add( final FileList fileList )
    {
        m_fileList.add( fileList );
    }

    /**
     * Returns the files in the list.
     */
    public FileObject[] listFiles( final TaskContext context )
        throws TaskException
    {
        if( m_selector == null )
        {
            throw new TaskException( "filteredfilelist.no-selector.error" );
        }

        // Build the set of files
        final ArrayList acceptedFiles = new ArrayList();
        final FileObject[] files = m_fileList.listFiles( context );
        for( int i = 0; i < files.length; i++ )
        {
            final FileObject file = files[ i ];
            if( m_selector.accept( file, null, context ) )
            {
                acceptedFiles.add( file );
            }
        }

        return (FileObject[])acceptedFiles.toArray( new FileObject[ acceptedFiles.size() ] );
    }
}
