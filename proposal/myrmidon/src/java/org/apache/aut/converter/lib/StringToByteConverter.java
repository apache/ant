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
 * String to byte converter
 *
 * <p>Hexadecimal numbers begin with 0x, Octal numbers begin with 0o and binary
 * numbers begin with 0b, all other values are assumed to be decimal.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.converter source="java.lang.String" destination="java.lang.Byte"
 */
public class StringToByteConverter
    extends AbstractConverter
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( StringToByteConverter.class );

    public StringToByteConverter()
    {
        super( String.class, Byte.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        try
        {
            final String value = (String)object;
            byte result = 0;
            if( value.startsWith( "0x" ) )
            {
                result = Byte.parseByte( value.substring( 2 ), 16 );
            }
            else if( value.startsWith( "0o" ) )
            {
                result = Byte.parseByte( value.substring( 2 ), 8 );
            }
            else if( value.startsWith( "0b" ) )
            {
                result = Byte.parseByte( value.substring( 2 ), 2 );
            }
            else
            {
                result = Byte.parseByte( value );
            }
            return new Byte( result );
        }
        catch( final NumberFormatException nfe )
        {
            final String message = REZ.getString( "convert.bad-byte.error", object );
            throw new ConverterException( message, nfe );
        }
    }
}

