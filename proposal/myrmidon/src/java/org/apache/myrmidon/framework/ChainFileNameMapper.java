/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A mapper that applies a chain of mappers.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="mapper" name="chain"
 */
public class ChainFileNameMapper
    implements FileNameMapper
{
    private final ArrayList m_mappers = new ArrayList();

    /**
     * Adds a nested mapper.
     */
    public void add( final FileNameMapper mapper )
    {
        m_mappers.add( mapper );
    }

    /**
     * Returns an array containing the target filename(s) for the given source
     * file.
     */
    public String[] mapFileName( final String sourceFileName,
                                 final TaskContext context )
        throws TaskException
    {
        ArrayList names = new ArrayList();
        names.add( sourceFileName );

        final int count = m_mappers.size();
        for( int i = 0; i < count; i++ )
        {
            final FileNameMapper mapper = (FileNameMapper)m_mappers.get( i );
            names = mapNames( mapper, names, context );
        }

        return (String[])names.toArray( new String[ names.size() ] );
    }

    /**
     * Maps a set of names.
     */
    private ArrayList mapNames( final FileNameMapper mapper,
                                final ArrayList names,
                                final TaskContext context )
        throws TaskException
    {
        final ArrayList retval = new ArrayList();

        // Map each of the supplied names
        final int count = names.size();
        for( int i = 0; i < count; i++ )
        {
            final String name = (String)names.get( i );
            final String[] newNames = mapper.mapFileName( name, context );
            if( newNames == null )
            {
                continue;
            }
            for( int j = 0; j < newNames.length; j++ )
            {
                final String newName = newNames[ j ];
                retval.add( newName );
            }
        }

        return retval;
    }
}
