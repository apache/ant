/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.converter.AbstractConverter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * String to short converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class StringToShortConverter
    extends AbstractConverter
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( StringToShortConverter.class );

    public StringToShortConverter()
    {
        super( String.class, Short.class );
    }

    public Object convert( final Object object, final Context context )
        throws ConverterException
    {
        try { return new Short( (String)object ); }
        catch( final NumberFormatException nfe )
        {
            final String message = REZ.getString( "convert.bad-short.error", object );
            throw new ConverterException( message, nfe );
        }
    }
}

