/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.aut.converter.Converter;
import org.apache.aut.converter.ConverterException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * This is an abstract implementation of a <code>MasterConverter</code>.
 * A MasterConverter is capable of converting between many different
 * source and destination types. The <code>MasterConverter</code>
 * delegates to other converters that do the actual work.
 *
 * <p>To use this class you must subclass it, overide the
 * (@link #createConverter(String)) method and register some
 * converters using the (@link #registerConverter(String,String,String))
 * method.</p>
 *
 * <p>The reason this class deals with strings rather than Class objects
 * is because dealing with strings allows us to implement alternative
 * mechanisms for defining Converters in the future, only defining converter
 * when it is first used.</p>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class AbstractMasterConverter
    implements Converter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultMasterConverter.class );

    /**
     * Map from converter classname to instance of converter.
     */
    private final Map m_converters = new HashMap();

    /**
     * This holds the mapping between source/destination
     * and converter name.
     */
    private final HashMap m_mapping = new HashMap();

    /**
     * Convert object to destination type.
     *
     * @param destination the destination type
     * @param original the original object
     * @param context the context in which to convert
     * @return the converted object
     * @exception ConverterException if an error occurs
     */
    public Object convert( final Class destination,
                           final Object original,
                           final Object context )
        throws ConverterException
    {
        final Class originalClass = original.getClass();

        if( destination.isAssignableFrom( originalClass ) )
        {
            return original;
        }

        try
        {
            // Search inheritance hierarchy for converter
            final String name = findConverter( originalClass, destination );

            // Create the converter
            Converter converter = (Converter)m_converters.get( name );
            if( converter == null )
            {
                converter = createConverter( name );
                m_converters.put( name, converter );
            }

            // Convert
            final Object object = converter.convert( destination, original, context );
            if( destination.isInstance( object ) )
            {
                return object;
            }

            final String message =
                REZ.getString( "bad-return-type.error",
                               object.getClass().getName(),
                               destination.getName() );
            throw new ConverterException( message );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "convert.error",
                                                  originalClass.getName(),
                                                  destination.getName() );
            throw new ConverterException( message, e );
        }
    }

    /**
     * Register a converter
     *
     * @param classname the className of converter
     * @param source the source classname
     * @param destination the destination classname
     */
    protected void registerConverter( final String classname,
                                      final String source,
                                      final String destination )
    {
        HashMap map = (HashMap)m_mapping.get( source );
        if( null == map )
        {
            map = new HashMap();
            m_mapping.put( source, map );
        }

        map.put( destination, classname );

        //Remove instance of converter if it has already been created
        m_converters.remove( classname );
    }

    /**
     * Create an instance of converter with specified name.
     *
     * @param name the name of converter
     * @return the created converter instance
     * @throws Exception if converter can not be created.
     */
    protected abstract Converter createConverter( final String name )
        throws Exception;

    /**
     * Determine the name of the converter to use to convert between
     * original and destination classes.
     */
    private String findConverter( final Class originalClass,
                                  final Class destination )
        throws ConverterException
    {
        //TODO: Maybe we should search the destination classes hierarchy as well

        // Recursively iterate over the super-types of the original class,
        // looking for a converter from source type -> destination type.
        // If more than one is found, choose the most specialised.

        Class match = null;
        String converterName = null;
        ArrayList queue = new ArrayList();
        queue.add( originalClass );

        while( !queue.isEmpty() )
        {
            Class clazz = (Class)queue.remove( 0 );

            // Add superclass and all interfaces
            if( clazz.getSuperclass() != null )
            {
                queue.add( clazz.getSuperclass() );
            }
            final Class[] interfaces = clazz.getInterfaces();
            for( int i = 0; i < interfaces.length; i++ )
            {
                queue.add( interfaces[ i ] );
            }

            // Check if we can convert from current class to destination
            final String name = getConverterClassname( clazz.getName(),
                                                       destination.getName() );
            if( name == null )
            {
                continue;
            }

            // Choose the more specialised source class
            if( match == null || match.isAssignableFrom( clazz ) )
            {
                match = clazz;
                converterName = name;
            }
            else if( clazz.isAssignableFrom( clazz ) )
            {
                continue;
            }
            else
            {
                // Duplicate
                final String message = REZ.getString( "ambiguous-converter.error" );
                throw new ConverterException( message );
            }
        }

        // TODO - should cache the (src, dest) -> converter mapping
        if( match != null )
        {
            return converterName;
        }

        // Could not find a converter
        final String message = REZ.getString( "no-converter.error" );
        throw new ConverterException( message );
    }

    /**
     * Retrieve name of ConverterInfo that describes converter that converts
     * from source to destination.
     *
     * @param source the source classname
     * @param destination the destination classname
     * @return the className of converter or null if none available
     */
    private String getConverterClassname( final String source, final String destination )
    {
        final HashMap map = (HashMap)m_mapping.get( source );
        if( null == map )
        {
            return null;
        }
        return (String)map.get( destination );
    }
}
