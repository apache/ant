/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * String to url converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:converter source="java.lang.String" destination="java.net.URL"
 */
public class StringToURLConverter
    extends AbstractConverter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToURLConverter.class );

    public StringToURLConverter()
    {
        super( String.class, URL.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        try
        {
            return new URL( (String)object );
        }
        catch( final MalformedURLException mue )
        {
            final String message = REZ.getString( "convert.bad-url.error", object );
            throw new ConverterException( message, mue );
        }

    }
}

