/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.util.FileUtils;

/**
 * Used for nested xml command line definitions.
 */
public class Argument
{
    private String[] m_parts;

    public Argument()
    {
    }

    public Argument( final String value )
    {
        setValue( value );
    }

    public Argument( final File file )
    {
        setFile( file );
    }

    /**
     * Sets a single commandline argument to the absolute filename of the
     * given file.
     *
     * @param value a single commandline argument.
     */
    public void setFile( final File value )
    {
        m_parts = new String[]{value.getAbsolutePath()};
    }

    /**
     * Line to split into several commandline arguments.
     *
     * @param line line to split into several commandline arguments
     */
    public void setLine( final String line )
        throws TaskException
    {
        m_parts = FileUtils.translateCommandline( line );
    }

    /**
     * Sets a single commandline argument and treats it like a PATH -
     * ensures the right separator for the local platform is used.
     *
     * @param value a single commandline argument.
     */
    public void setPath( final Path value )
    {
        m_parts = new String[]{value.toString()};
    }

    /**
     * Sets a single commandline argument.
     *
     * @param value a single commandline argument.
     */
    public void setValue( final String value )
    {
        m_parts = new String[]{value};
    }

    /**
     * Returns the parts this Argument consists of.
     *
     * @return The Parts value
     */
    public String[] getParts()
    {
        return m_parts;
    }
}
