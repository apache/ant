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
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.converter.ConverterException;

/**
 * Converter engine to handle converting between types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultMasterConverter
    extends AbstractLoggable
    implements MasterConverter, Composable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultMasterConverter.class );

    private final static boolean DEBUG                = false;

    private ConverterRegistry    m_registry;
    private TypeFactory          m_factory;

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
        try { m_factory = typeManager.getFactory( Converter.ROLE ); }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "no-converter-factory.error" );
            throw new ComponentException( message, te );
        }
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
            final String message = 
                REZ.getString( "converter-lookup.notice", 
                               originalClass.getName(), 
                               destination.getName() );
            getLogger().debug( message );
        }

        //TODO: Start searching inheritance hierarchy for converter
        final String name = m_registry.getConverterName( originalClass.getName(),
                                                         destination.getName() );

        if( null == name )
        {
            final String message = 
                REZ.getString( "no-converter.notice", 
                               originalClass.getName(), 
                               destination.getName() );
            throw new ConverterException( message );
        }

        try
        {
            //TODO: Start caching converters instead of repeatedly instantiating em.
            final Converter converter = (Converter)m_factory.create( name );

            if( DEBUG )
            {
                final String message = REZ.getString( "found-converter.notice", converter );
                getLogger().debug( message );
            }

            return converter.convert( destination, original, context );
        }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "bad-typemanager.error" );
            throw new ConverterException( message, te );
        }
    }
}
