/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

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

    public AbstractConverter( final Class source, final Class destination )
    {
        m_source = source;
        m_destination = destination;
    }

    public Object convert( final Class destination, final Object original )
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
            
        return convert( original );
    }

    protected abstract Object convert( Object original )
        throws Exception;
}

