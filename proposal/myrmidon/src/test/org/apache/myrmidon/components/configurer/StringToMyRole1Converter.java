/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;
import org.apache.avalon.framework.context.Context;

/**
 * Converts from a string to a {@link MyRole1} implementation.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class StringToMyRole1Converter
    extends AbstractConverter
{
    public StringToMyRole1Converter()
    {
        super( String.class, MyRole1.class );
    }

    protected Object convert( Object original, Context context )
        throws ConverterException
    {
        return new MyType1();
    }
}
