/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.mappers;

import java.io.File;

/**
 * Implementation of FileNameMapper that always returns the source file name
 * without any leading directory information. <p>
 *
 * This is the default FileNameMapper for the copy and move tasks if the flatten
 * attribute has been set.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class FlatFileNameMapper
    implements FileNameMapper
{
    /**
     * Ignored.
     */
    public void setFrom( final String from )
    {
    }

    /**
     * Ignored.
     */
    public void setTo( final String to )
    {
    }

    /**
     * Returns an one-element array containing the source file name without any
     * leading directory information.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( final String sourceFileName )
    {
        return new String[]{new File( sourceFileName ).getName()};
    }
}
