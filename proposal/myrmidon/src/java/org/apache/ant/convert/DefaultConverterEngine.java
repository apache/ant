/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.ant.AntException;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.Context;
import org.apache.avalon.camelot.DefaultFactory;
import org.apache.avalon.camelot.DefaultLocatorRegistry;
import org.apache.avalon.camelot.Factory;
import org.apache.avalon.camelot.Locator;
import org.apache.avalon.camelot.LocatorRegistry;

/**
 * Converter engine to handle converting between types.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultConverterEngine
    extends AbstractLoggable
    implements ConverterEngine, Composer
{
    protected final static boolean DEBUG                = false;

    protected Factory              m_factory;
    protected LocatorRegistry      m_registry      = new DefaultLocatorRegistry();
    protected ConverterRegistry    m_infoRegistry  = new DefaultConverterRegistry();

    /**
     * Get registry used to locate converters.
     *
     * @return the LocatorRegistry
     */
    public LocatorRegistry getRegistry()
    {
        return m_registry;
    }
    
    /**
     * Get registry for converterInfo objects.
     *
     * @return the ConverterRegistry
     */
    public ConverterRegistry getInfoRegistry()
    {
        return m_infoRegistry;
    }
    
    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentManagerException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentManagerException
    {
        m_factory = (Factory)componentManager.lookup( "org.apache.avalon.camelot.Factory" );
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
        throws Exception
    {
        final Class originalClass = original.getClass();

        if( destination.isAssignableFrom( originalClass ) )
        {
            return original;
        }

        if( DEBUG )
        {
            m_logger.debug( "Looking for converter from " + originalClass.getName() +
                            " to " + destination.getName() );
        }

        //TODO: Start searching inheritance hierarchy for converter
        final String name = 
            m_infoRegistry.getConverterInfoName( originalClass.getName(), 
                                                 destination.getName() );
            
        if( null == name ) 
        {
            throw new ConverterException( "Unable to find converter for " + 
                                          originalClass.getName() + " to " + 
                                          destination.getName() + " conversion" );
        }

        //TODO: Start caching converters instead of repeatedly instantiating em.
        final Locator locator = m_registry.getLocator( name );
        final Converter converter = (Converter)m_factory.create( locator, Converter.class );
        return converter.convert( destination, original, context );
    }
}
