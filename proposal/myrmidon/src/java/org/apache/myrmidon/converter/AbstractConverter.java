/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.converter;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;

/**
 * Instances of this interface are used to convert between different types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class AbstractConverter
    implements Converter
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( AbstractConverter.class );

    private final Class m_source;
    private final Class m_destination;

    /**
     * Constructor for a converter between types source and destination
     *
     * @param source the source type
     * @param destination the destination type
     */
    public AbstractConverter( final Class source, final Class destination )
    {
        m_source = source;
        m_destination = destination;
    }

    /**
     * Convert an object from original to destination types
     *
     * @param destination the destination type
     * @param original the original Object
     * @param context the context in which to convert
     * @return the converted object
     * @exception Exception if an error occurs
     */
    public Object convert( final Class destination,
                           final Object original,
                           final Context context )
        throws ConverterException
    {
        if( m_destination != destination )
        {
            final String message =
                REZ.getString( "bad-destination.error", destination.getName(), m_destination );
            throw new IllegalArgumentException( message );
        }

        if( !m_source.isInstance( original ) )
        {
            final String message =
                REZ.getString( "bad-instance.error", original, m_source.getName() );
            throw new IllegalArgumentException( message );
        }

        return convert( original, context );
    }

    /**
     * Overide this in a particular converter to do the conversion.
     *
     * @param original the original Object
     * @param context the context in which to convert
     * @return the converted object
     * @exception Exception if an error occurs
     */
    protected abstract Object convert( Object original, Context context )
        throws ConverterException;
}

