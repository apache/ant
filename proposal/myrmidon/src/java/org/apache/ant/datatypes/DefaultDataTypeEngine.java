/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.datatypes;

import org.apache.avalon.Composer;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentManagerException;
import org.apache.avalon.Composer;
import org.apache.avalon.camelot.DefaultLocatorRegistry;
import org.apache.avalon.camelot.Factory;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.Locator;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.avalon.camelot.RegistryException;

/**
 * This is basically a engine that can be used to access data-types.
 * The engine acts as a repository and factory for these types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultDataTypeEngine
    implements DataTypeEngine, Composer
{
    protected Factory              m_factory;
    protected LocatorRegistry      m_registry  = new DefaultLocatorRegistry();
    
    /**
     * Retrieve registry of data-types.
     * This is used by deployer to add types into engine.
     *
     * @return the registry
     */
    public LocatorRegistry getRegistry()
    {
        return m_registry;
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
     * Create a data-type of type registered under name.
     *
     * @param name the name of data type
     * @return the DataType
     * @exception RegistryException if an error occurs
     * @exception FactoryException if an error occurs
     */    
    public DataType createDataType( final String name )
         throws RegistryException, FactoryException
    {
        final Locator locator = m_registry.getLocator( name );
        return (DataType)m_factory.create( locator, DataType.class );
    }
}
