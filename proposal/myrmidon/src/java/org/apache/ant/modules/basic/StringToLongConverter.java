/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * String to long converter
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class StringToLongConverter
    extends AbstractConverter
{
    public StringToLongConverter()
    {
        super( String.class, Long.class );
    }

    public Object convert( final Object original, final Context context )
        throws ConverterException
    {
        try { return new Long( (String)original ); }
        catch( final NumberFormatException nfe )
        {
            throw new ConverterException( "Error formatting object", nfe );
        }
    }
}

