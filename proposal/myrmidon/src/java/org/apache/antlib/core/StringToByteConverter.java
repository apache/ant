/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * String to byte converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
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

    public Object convert( final Object object, final Context context )
        throws ConverterException
    {
        try { return new Byte( (String)object ); }
        catch( final NumberFormatException nfe )
        {
            final String message = REZ.getString( "convert.bad-byte.error", object );
            throw new ConverterException( message, nfe );
        }
    }
}

