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
import org.apache.log.Logger;

public class DefaultConverterEngine
    implements ConverterEngine, Initializable
{
    protected ConverterFactory     m_converterFactory;
    protected ConverterRegistry    m_converterRegistry;
    protected Logger               m_logger;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public ConverterRegistry getConverterRegistry()
    {
        return m_converterRegistry;
    }

    public ConverterFactory getConverterFactory()
    {
        return m_converterFactory;
    }

    public void init()
        throws Exception
    {
        m_converterRegistry = createConverterRegistry();
        m_converterFactory =  createConverterFactory();
    }
    
    protected ConverterRegistry createConverterRegistry()
    {
        return new DefaultConverterRegistry();
    }

    protected ConverterFactory createConverterFactory()
    {
        return new DefaultConverterFactory();
    }

    public Object convert( Class destination, final Object original )
        throws Exception
    {
        final ConverterInfo info = 
            m_converterRegistry.getConverterInfo( original.getClass().getName(), 
                                                  destination.getName() );
            
        if( null == info ) 
        {
            throw new ConverterException( "Unable to find converter for " + 
                                          original.getClass() + " to " + destination + 
                                          " conversion" );
        }

        final ConverterEntry entry = m_converterFactory.create( info );
        final Converter converter = entry.getConverter();
        return converter.convert( destination, original );
    }
}
