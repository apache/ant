/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.myrmidon.framework.file.Path;

/**
 * A converter from String to Path.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:converter source="java.lang.String" destination="org.apache.myrmidon.framework.file.Path"
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
     */
    protected Object convert( final Object original, final Object context )
        throws ConverterException
    {
        String path = (String)original;
        Path retval = new Path( path );
        return retval;
    }
}

