/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.util.ArrayList;
import org.apache.aut.vfs.FileObject;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.ChainFileNameMapper;

/**
 * A fileset that maps another fileset.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.data-type name="mapped-fileset"
 */
public class MappedFileSet
    implements FileSet
{
    private final ArrayList m_filesets = new ArrayList();
    private ChainFileNameMapper m_mapper = new ChainFileNameMapper();

    /**
     * Sets the mapper to use.
     */
    public void setMapper( final ChainFileNameMapper mapper )
    {
        m_mapper.add( mapper );
    }

    /**
     * Sets the fileset to map.
     */
    public void add( final FileSet fileset )
    {
        m_filesets.add( fileset );
    }

    /**
     * Returns the contents of the set.
     */
    public FileSetResult getResult( final TaskContext context )
        throws TaskException
    {
        final DefaultFileSetResult result = new DefaultFileSetResult();

        // Map each source fileset.
        final int count = m_filesets.size();
        for( int i = 0; i < count; i++ )
        {
            final FileSet fileSet = (FileSet)m_filesets.get(i );
            mapFileSet( fileSet, result, context );
        }

        return result;
    }

    /**
     * Maps the contents of a fileset.
     */
    private void mapFileSet( final FileSet fileset,
                             final DefaultFileSetResult result,
                             final TaskContext context )
        throws TaskException
    {
        // Build the result from the nested fileset
        FileSetResult origResult = fileset.getResult( context );
        final FileObject[] files = origResult.getFiles();
        final String[] paths = origResult.getPaths();

        // Map each element of the result
        for( int i = 0; i < files.length; i++ )
        {
            final FileObject file = files[ i ];
            final String path = paths[ i ];
            String[] newPaths = m_mapper.mapFileName( path, context );
            if( newPaths == null )
            {
                continue;
            }
            for( int j = 0; j < newPaths.length; j++ )
            {
                String newPath = newPaths[j ];
                result.addElement( file, newPath );
            }
        }
    }
}
