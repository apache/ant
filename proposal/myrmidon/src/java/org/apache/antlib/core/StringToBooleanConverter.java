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
import org.apache.aut.converter.AbstractConverter;
import org.apache.aut.converter.ConverterException;

/**
 * String to boolean converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:converter source="java.lang.String" destination="java.lang.Boolean"
 */
public class StringToBooleanConverter
    extends AbstractConverter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToBooleanConverter.class );

    public StringToBooleanConverter()
    {
        super( String.class, Boolean.class );
    }

    public Object convert( final Object object, final Object context )
        throws ConverterException
    {
        final String string = (String)object;
        if( string.equals( "true" ) )
        {
            return Boolean.TRUE;
        }
        else if( string.equals( "false" ) )
        {
            return Boolean.FALSE;
        }
        else
        {
            final String message = REZ.getString( "convert.bad-boolean.error", object );
            throw new ConverterException( message );
        }
    }
}

