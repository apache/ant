/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.converter.lib;

import java.util.Date;
import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * String to date converter.
 *
 * <p>Parses a date according to the same rules as the Date.parse() method. In
 * particular it recognizes the IETF standard date syntax:</p>
 * <p>"Sat, 12 Aug 1995 13:30:00 GMT"</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.converter source="java.lang.String" destination="java.util.Date"
 * @see java.util.Date#parse
 */
public class StringToDateConverter
    extends AbstractConverter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToDateConverter.class );

    public StringToDateConverter()
    {
        super( String.class, Date.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        try
        {
            return new Date( object.toString() );
        }
        catch( final NumberFormatException nfe )
        {
            final String message = REZ.getString( "convert.bad-byte.error", object );
            throw new ConverterException( message, nfe );
        }
    }
}

