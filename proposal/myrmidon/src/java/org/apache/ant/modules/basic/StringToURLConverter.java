/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.modules.basic;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * String to url converter
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class StringToURLConverter
    extends AbstractConverter
{
    public StringToURLConverter()
    {
        super( String.class, URL.class );
    }

    public Object convert( final Object original, final Context context )
        throws ConverterException
    {
        try { return new URL( (String)original ); }
        catch( final MalformedURLException mue )
        {
            throw new ConverterException( "Error formatting object", mue );
        }

    }
}

