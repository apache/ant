/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.avalon.Context;

/**
 * Instances of this interface are used to convert between different types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractConverter
    implements Converter
{
    protected final Class         m_source;
    protected final Class         m_destination;

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
    public Object convert( final Class destination, final Object original, Context context )
        throws Exception
    {
        if( m_destination != destination )
        {
            throw new IllegalArgumentException( "Destination type " + destination.getName() +
                                                " is not equal to " + m_destination );
        }

        if( !m_source.isInstance( original ) )
        {
            throw new IllegalArgumentException( "Object '" + original + "' is not an " + 
                                                "instance of " + m_source.getName() );
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
        throws Exception;
}

