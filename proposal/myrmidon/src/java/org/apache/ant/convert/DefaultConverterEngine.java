/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.convert;

import org.apache.ant.AntException;
import org.apache.avalon.Component;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.DefaultFactory;
import org.apache.avalon.camelot.DefaultLocatorRegistry;
import org.apache.avalon.camelot.Locator;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.log.Logger;

public class DefaultConverterEngine
    implements ConverterEngine, Initializable
{
    protected final static boolean DEBUG                = false;
    protected DefaultFactory       m_factory;
    protected LocatorRegistry      m_registry;
    protected ConverterRegistry    m_infoRegistry;
    protected Logger               m_logger;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public LocatorRegistry getRegistry()
    {
        return m_registry;
    }

    public ConverterRegistry getInfoRegistry()
    {
        return m_infoRegistry;
    }

    public void init()
        throws Exception
    {
        m_infoRegistry = createInfoRegistry();
        m_registry = createRegistry();
        m_factory =  createFactory();
    }
    
    protected ConverterRegistry createInfoRegistry()
    {
        return new DefaultConverterRegistry();
    }

    protected LocatorRegistry createRegistry()
    {
        return new DefaultLocatorRegistry();
    }

    protected DefaultFactory createFactory()
    {
        return new DefaultFactory();
    }

    public Object convert( Class destination, final Object original )
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

        final String name = 
            m_infoRegistry.getConverterInfoName( originalClass.getName(), 
                                                 destination.getName() );
            
        if( null == name ) 
        {
            throw new ConverterException( "Unable to find converter for " + 
                                          originalClass.getName() + " to " + 
                                          destination.getName() + " conversion" );
        }

        final Locator locator = m_registry.getLocator( name );
        final Converter converter = (Converter)m_factory.create( locator, Converter.class );
        return converter.convert( destination, original );
    }
}
