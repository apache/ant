/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.mappers;

/**
 * Implementation of FileNameMapper that always returns the same target file
 * name. <p>
 *
 * This is the default FileNameMapper for the archiving tasks and uptodate.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class MergingMapper
    implements FileNameMapper
{
    private String[] m_mergedFile;

    /**
     * Ignored.
     *
     * @param from The new From value
     */
    public void setFrom( String from )
    {
    }

    /**
     * Sets the name of the merged file.
     *
     * @param to The new To value
     */
    public void setTo( String to )
    {
        m_mergedFile = new String[]{to};
    }

    /**
     * Returns an one-element array containing the file name set via setTo.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( final String sourceFileName )
    {
        return m_mergedFile;
    }
}
