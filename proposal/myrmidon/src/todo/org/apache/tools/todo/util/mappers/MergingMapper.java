/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.util.mappers;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileNameMapper;

/**
 * Implementation of FileNameMapper that always returns the same target file
 * name. <p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant.type type="mapper" name="merge"
 */
public class MergingMapper
    implements FileNameMapper
{
    private String[] m_mergedFile;

    /**
     * Sets the name of the merged file.
     *
     * @param to The new To value
     */
    public void setTo( final String to )
    {
        m_mergedFile = new String[]{ to };
    }

    /**
     * Returns an one-element array containing the file name set via setTo.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( final String sourceFileName, TaskContext context )
        throws TaskException
    {
        if( m_mergedFile == null )
        {
            throw new TaskException( "Destination file was not specified." );
        }
        return m_mergedFile;
    }
}
