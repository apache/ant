/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.converter;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentSelector;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.myrmidon.components.converter.MasterConverter;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * Converter engine to handle converting between types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultMasterConverter
    extends AbstractLoggable
    implements MasterConverter, Composable
{
    private final static boolean DEBUG                = false;

    private ConverterRegistry    m_registry;
    private ComponentSelector    m_selector;

    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_registry = (ConverterRegistry)componentManager.lookup( ConverterRegistry.ROLE );

        final TypeManager typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_selector = (ComponentSelector)typeManager.lookup( Converter.ROLE + "Selector" );
    }

    /**
     * Convert object to destination type.
     *
     * @param destination the destination type
     * @param original the original object
     * @param context the context in which to convert
     * @return the converted object
     * @exception Exception if an error occurs
     */
    public Object convert( Class destination, final Object original, final Context context )
        throws ConverterException
    {
        final Class originalClass = original.getClass();

        if( destination.isAssignableFrom( originalClass ) )
        {
            return original;
        }

        if( DEBUG )
        {
            getLogger().debug( "Looking for converter from " + originalClass.getName() +
                               " to " + destination.getName() );
        }

        //TODO: Start searching inheritance hierarchy for converter
        final String name = m_registry.getConverterName( originalClass.getName(),
                                                         destination.getName() );

        if( null == name )
        {
            throw new ConverterException( "Unable to find converter for " +
                                          originalClass.getName() + " to " +
                                          destination.getName() + " conversion" );
        }

        try
        {
            //TODO: Start caching converters instead of repeatedly instantiating em.
            final Converter converter = (Converter)m_selector.select( name );


            if( DEBUG )
            {
                getLogger().debug( "Found Converter: " + converter );
            }

            return converter.convert( destination, original, context );
        }
        catch( final ComponentException ce )
        {
            throw new ConverterException( "Badly configured TypeManager missing " +
                                          "converter definition" );
        }
    }
}
