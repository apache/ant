/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types.converters;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.tools.todo.types.Path;

/**
 * A converter from String to Path.
 *
 * @author Adam Murdoch
 */
public class StringToPathConverter
    extends AbstractConverter
{
    /**
     * Constructors a converter.
     */
    public StringToPathConverter()
    {
        super( String.class, Path.class );
    }

    /**
     * Converts from String to Path
     *
     * @param original the original Object
     * @param context the context in which to convert
     * @return the converted object
     * @exception java.lang.Exception if an error occurs
     */
    protected Object convert( Object original, Object context )
        throws ConverterException
    {
        /*
        String path = (String)original;
        TaskContext taskContext = (TaskContext)context;

        Path retval = new Path( path );
        retval.setBaseDirectory( taskContext.getBaseDirectory() );
        return retval;
        */
        return null;
    }
}

