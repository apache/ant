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
 * String to float converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.converter source="java.lang.String" destination="java.lang.Float"
 */
public class StringToFloatConverter
    extends AbstractConverter
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( StringToFloatConverter.class );

    public StringToFloatConverter()
    {
        super( String.class, Float.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        try
        {
            return new Float( (String)object );
        }
        catch( final NumberFormatException nfe )
        {
            final String message = REZ.getString( "convert.bad-float.error", object );
            throw new ConverterException( message, nfe );
        }
    }
}

