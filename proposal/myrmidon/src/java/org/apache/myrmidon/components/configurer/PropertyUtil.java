/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;

/**
 * Utility class to evaluate propertys.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class PropertyUtil
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( PropertyUtil.class );

    private PropertyUtil()
    {
    }

    /**
     * Resolve a string property. This evaluates all property
     * substitutions based on specified context.
     *
     * @param property the property to resolve
     * @param context the context in which to resolve property
     * @param ignoreUndefined if false will throw an PropertyException if property is not found
     * @return the reolved property
     * @exception PropertyException if an error occurs
     */
    public static Object resolveProperty( final String property,
                                          final Context context,
                                          final boolean ignoreUndefined )
        throws PropertyException
    {
        int start = findBeginning( property, 0 );
        if( -1 == start )
        {
            return property;
        }

        int end = findEnding( property, start );

        final int length = property.length();

        if( 0 == start && end == ( length - 1 ) )
        {
            return resolveValue( property.substring( start + 2, end ),
                                 context,
                                 ignoreUndefined );
        }

        final StringBuffer sb = new StringBuffer( length * 2 );
        int lastPlace = 0;

        while( true )
        {
            final Object value =
                resolveValue( property.substring( start + 2, end ),
                              context,
                              ignoreUndefined );

            sb.append( property.substring( lastPlace, start ) );
            sb.append( value );

            lastPlace = end + 1;

            start = findBeginning( property, lastPlace );
            if( -1 == start )
            {
                break;
            }

            end = findEnding( property, start );
        }

        sb.append( property.substring( lastPlace, length ) );

        return sb.toString();
    }

    /**
     * Resolve a string property. This recursively evaluates all property
     * substitutions based on specified context.
     *
     * @param property the property to resolve
     * @param context the context in which to resolve property
     * @param ignoreUndefined if false will throw an PropertyException if property is not found
     * @return the reolved property
     * @exception PropertyException if an error occurs
     */
    public static Object recursiveResolveProperty( final String property,
                                                   final Context context,
                                                   final boolean ignoreUndefined )
        throws PropertyException
    {
        int start = findBeginning( property, 0 );
        if( -1 == start )
        {
            return property;
        }

        int end = findNestedEnding( property, start );

        final int length = property.length();

        if( 0 == start && end == ( length - 1 ) )
        {
            final String propertyName = property.substring( start + 2, end );
            final Object key = recursiveResolveProperty( propertyName, context, ignoreUndefined );
            return resolveValue( key.toString(), context, ignoreUndefined );
        }

        final StringBuffer sb = new StringBuffer( length * 2 );

        int lastPlace = 0;

        while( true )
        {
            final String propertyName = property.substring( start + 2, end );
            final Object key = recursiveResolveProperty( propertyName, context, ignoreUndefined );
            final Object value = resolveValue( key.toString(), context, ignoreUndefined );

            sb.append( property.substring( lastPlace, start ) );
            sb.append( value );

            lastPlace = end + 1;

            start = findBeginning( property, lastPlace );
            if( -1 == start )
            {
                break;
            }

            end = findNestedEnding( property, start );
        }

        sb.append( property.substring( lastPlace, length ) );

        return sb.toString();
    }

    private static int findBeginning( final String property, final int currentPosition )
    {
        //TODO: Check if it is commented out
        return property.indexOf( "${", currentPosition );
    }

    private static int findEnding( final String property, final int currentPosition )
        throws PropertyException
    {
        //TODO: Check if it is commented out
        final int index = property.indexOf( '}', currentPosition );
        if( -1 == index )
        {
            final String message = REZ.getString( "prop.mismatched-braces.error" );
            throw new PropertyException( message );
        }

        return index;
    }

    private static int findNestedEnding( final String property, final int currentPosition )
        throws PropertyException
    {
        final int length = property.length();
        final int start = currentPosition + 2;

        int weight = 1;
        for( int i = start; ( weight > 0 ) && ( i < length ); i++ )
        {
            final char ch = property.charAt( i );
            switch( ch )
            {
                case '}':
                    //TODO: Check if it is commented out
                    weight--;
                    if( weight == 0 )
                    {
                        return i;
                    }
                    break;

                case '$':
                    {
                        //TODO: Check if it is commented out
                        final int next = i + 1;
                        if( next < length && '{' == property.charAt( next ) )
                        {
                            weight++;
                        }
                    }
                    break;
            }
        }

        final String message = REZ.getString( "prop.mismatched-braces.error" );
        throw new PropertyException( message );
    }

    /**
     * Retrieve a value from the specified context using the specified key.
     * If there is no such value and ignoreUndefined is not false then a
     * PropertyException is generated.
     *
     * @param key the key of value in context
     * @param context the Context
     * @param ignoreUndefined true if undefined variables are ignored
     * @return the object retrieved from context
     * @exception PropertyException if an error occurs
     */
    private static Object resolveValue( final String key,
                                        final Context context,
                                        final boolean ignoreUndefined )
        throws PropertyException
    {
        try
        {
            return context.get( key );
        }
        catch( final ContextException ce )
        {
            if( ignoreUndefined )
            {
                return "";
            }
            else
            {
                final String message =
                    REZ.getString( "prop.missing-value.error", key );
                throw new PropertyException( message );
            }
        }
    }
}

