/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;

/**
 * A general-purpose converter that converts an Object to a String using
 * its toString() method.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:converter source="java.lang.Object" destination="java.lang.String"
 */
public class ObjectToStringConverter
    extends AbstractConverter
{
    public ObjectToStringConverter()
    {
        super( Object.class, String.class );
    }

    /**
     * Converts an object.
     */
    protected Object convert( final Object original, final Object context )
        throws ConverterException
    {
        return original.toString();
    }
}
