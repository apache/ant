/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.converter.lib;

import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * String to long converter
 *
 * <p>Hexadecimal numbers begin with 0x, Octal numbers begin with 0o and binary
 * numbers begin with 0b, all other values are assumed to be decimal.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.converter source="java.lang.String" destination="java.lang.Long"
 */
public class StringToLongConverter
    extends AbstractConverter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToLongConverter.class );

    public StringToLongConverter()
    {
        super( String.class, Long.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        try
        {
            final String value = (String)object;
            long result = 0;
            if( value.startsWith( "0x" ) )
            {
                result = Long.parseLong( value.substring( 2 ), 16 );
            }
            else if( value.startsWith( "0o" ) )
            {
                result = Long.parseLong( value.substring( 2 ), 8 );
            }
            else if( value.startsWith( "0b" ) )
            {
                result = Long.parseLong( value.substring( 2 ), 2 );
            }
            else
            {
                result = Long.parseLong( value );
            }
            return new Long( result );
        }
        catch( final NumberFormatException nfe )
        {
            final String message = REZ.getString( "convert.bad-long.error", object );
            throw new ConverterException( message, nfe );
        }
    }
}

