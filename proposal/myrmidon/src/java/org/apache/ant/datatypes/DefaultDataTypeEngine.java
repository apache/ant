/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.datatypes;

import org.apache.ant.AntException;
import org.apache.avalon.Initializable;
import org.apache.avalon.Loggable;
import org.apache.avalon.camelot.DefaultFactory;
import org.apache.avalon.camelot.DefaultLocatorRegistry;
import org.apache.avalon.camelot.Locator;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.avalon.camelot.RegistryException;
import org.apache.avalon.camelot.FactoryException;
import org.apache.log.Logger;

public class DefaultDataTypeEngine
    implements DataTypeEngine, Initializable
{
    protected DefaultFactory       m_factory;
    protected LocatorRegistry      m_registry;
    protected Logger               m_logger;
    
    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }
    
    public LocatorRegistry getRegistry()
    {
        return m_registry;
    }
    
    public void init()
        throws Exception
    {
        m_registry = createRegistry();
        setupComponent( m_registry );

        m_factory =  createFactory();
        setupComponent( m_factory );
    }

    protected void setupComponent( final Object object )
        throws Exception
    {
        if( object instanceof Loggable )
        {
            ((Loggable)object).setLogger( m_logger );
        }
    }
    
    protected LocatorRegistry createRegistry()
    {
        return new DefaultLocatorRegistry();
    }
    
    protected DefaultFactory createFactory()
    {
        return new DefaultFactory();
    }
    
    public DataType createDataType( final String name )
        throws RegistryException, FactoryException
    {
        final Locator locator = m_registry.getLocator( name );
        return (DataType)m_factory.create( locator, DataType.class );
    }
}
