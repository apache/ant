/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.converter;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.converter.ConverterException;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Converter engine to handle converting between types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultMasterConverter
    extends AbstractLogEnabled
    implements MasterConverter, Composable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultMasterConverter.class );

    private final static boolean DEBUG = false;

    private ConverterRegistry m_registry;
    private TypeFactory m_factory;

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
        try
        {
            m_factory = typeManager.getFactory( Converter.class );
        }
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
     * @exception ConverterException if an error occurs
     */
    public Object convert( final Class destination,
                           final Object original,
                           final Context context )
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

        //Searching inheritance hierarchy for converter
        final String name = getConverterName( originalClass, destination );

        try
        {
            //TODO: Start caching converters instead of repeatedly instantiating em.
            final Converter converter = (Converter)m_factory.create( name );

            if( DEBUG )
            {
                final String message = REZ.getString( "found-converter.notice", converter );
                getLogger().debug( message );
            }

            final Object object = converter.convert( destination, original, context );
            if( destination.isInstance( object ) )
            {
                return object;
            }
            else
            {
                final String message =
                    REZ.getString( "bad-return-type.error",
                                   name,
                                   object,
                                   destination.getName() );
                throw new ConverterException( message );
            }
        }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "bad-typemanager.error" );
            throw new ConverterException( message, te );
        }
    }

    private String getConverterName( final Class originalClass,
                                     final Class destination )
        throws ConverterException
    {
        //TODO: Maybe we should search the source classes hierarchy aswell
        for( Class clazz = destination;
             clazz != null;
             clazz = clazz.getSuperclass() )
        {
            final String name =
                m_registry.getConverterName( originalClass.getName(),
                                             clazz.getName() );
            if( name != null )
            {
                return name;
            }
        }

        final String message =
            REZ.getString( "no-converter.error",
                           originalClass.getName(),
                           destination.getName() );
        throw new ConverterException( message );
    }
}
