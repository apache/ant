/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util;

/**
 * Implementation of FileNameMapper that always returns the source file name.
 * <p>
 *
 * This is the default FileNameMapper for the copy and move tasks.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class IdentityMapper implements FileNameMapper
{

    /**
     * Ignored.
     *
     * @param from The new From value
     */
    public void setFrom( String from )
    {
    }

    /**
     * Ignored.
     *
     * @param to The new To value
     */
    public void setTo( String to )
    {
    }

    /**
     * Returns an one-element array containing the source file name.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( String sourceFileName )
    {
        return new String[]{sourceFileName};
    }
}
