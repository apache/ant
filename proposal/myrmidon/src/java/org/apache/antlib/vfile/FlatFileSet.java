/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A file set that flattens its contents into a single directory.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:data-type name="flat-fileset"
 * @ant:type type="v-fileset" name="flat-fileset"
 */
public class FlatFileSet
    implements FileSet
{
    private DefaultFileList m_files = new DefaultFileList();

    /**
     * Adds a file list to this set.
     */
    public void add( final FileList files )
    {
        m_files.add( files );
    }

    /**
     * Returns the contents of the set.
     */
    public FileSetResult getResult( final TaskContext context )
        throws TaskException
    {
        DefaultFileSetResult result = new DefaultFileSetResult();
        FileObject[] files = m_files.listFiles( context );
        for( int i = 0; i < files.length; i++ )
        {
            final FileObject file = files[ i ];

            // TODO - detect collisions
            result.addElement( file, file.getName().getBaseName() );
        }

        return result;
    }
}
