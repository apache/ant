/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;
import org.apache.aut.converter.Converter;
import org.apache.aut.converter.ConverterException;

/**
 * String to Enum converter
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:converter source="java.lang.String" destination="org.apache.avalon.framework.Enum"
 */
public class StringToEnumConverter
    implements Converter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( StringToEnumConverter.class );

    public Object convert( final Class destination,
                           final Object original,
                           final Object context )
        throws ConverterException
    {
        final Object object = getEnum( destination, original );
        if( null == object )
        {
            final String[] names = getValidNames( destination );
            final String message =
                REZ.getString( "invalid.enum.error", original, Arrays.asList( names ) );
            throw new ConverterException( message );
        }
        else
        {
            return object;
        }
    }

    private Object getEnum( final Class destination, final Object original )
        throws ConverterException
    {
        try
        {
            final Class[] types = new Class[]{String.class};
            final Object[] args = new Object[]{original.toString()};

            final Method method = destination.getMethod( "getByName", types );
            return method.invoke( null, args );
        }
        catch( final InvocationTargetException ite )
        {
            final Throwable target = ite.getTargetException();
            if( target instanceof IllegalArgumentException )
            {
                return null;
            }
            else
            {
                final String message =
                    REZ.getString( "getByName.error", destination.getName(), target );
                throw new ConverterException( message, target );
            }
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "enum.missing.getByName.error", destination.getName() );
            throw new ConverterException( message, e );
        }
    }

    private String[] getValidNames( final Class clazz )
        throws ConverterException
    {
        try
        {
            final Class[] types = new Class[ 0 ];
            final Object[] args = new Object[ 0 ];

            final Method method = clazz.getMethod( "getNames", types );
            return (String[])method.invoke( null, args );
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "enum.missing.getNames.error", clazz.getName() );
            throw new ConverterException( message, e );
        }
    }
}

