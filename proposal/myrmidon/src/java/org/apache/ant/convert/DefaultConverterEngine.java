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
    protected DefaultFactory       m_factory;
    protected LocatorRegistry      m_locatorRegistry;
    protected ConverterRegistry    m_converterRegistry;
    protected Logger               m_logger;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public LocatorRegistry getLocatorRegistry()
    {
        return m_locatorRegistry;
    }

    public ConverterRegistry getConverterRegistry()
    {
        return m_converterRegistry;
    }

    public void init()
        throws Exception
    {
        m_converterRegistry = createConverterRegistry();
        m_locatorRegistry = createLocatorRegistry();
        m_factory =  createFactory();
    }
    
    protected ConverterRegistry createConverterRegistry()
    {
        return new DefaultConverterRegistry();
    }

    protected LocatorRegistry createLocatorRegistry()
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
        final String name = 
            m_converterRegistry.getConverterInfoName( original.getClass().getName(), 
                                                      destination.getName() );
            
        if( null == name ) 
        {
            throw new ConverterException( "Unable to find converter for " + 
                                          original.getClass() + " to " + destination + 
                                          " conversion" );
        }

        final Locator locator = m_locatorRegistry.getLocator( name );
        final Converter converter = (Converter)m_factory.create( locator, Converter.class );
        return converter.convert( destination, original );
    }
}
