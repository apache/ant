/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.tasklet.DataType;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.camelot.FactoryException;
import org.apache.avalon.framework.camelot.Registry;
import org.apache.avalon.framework.camelot.RegistryException;

/**
 * This is basically a engine that can be used to access data-types.
 * The engine acts as a repository and factory for these types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface DataTypeEngine
    extends Component
{
    /**
     * Retrieve registry of data-types.
     * This is used by deployer to add types into engine.
     *
     * @return the registry
     */
    Registry getRegistry();

    /**
     * Create a data-type of type registered under name.
     *
     * @param name the name of data type
     * @return the DataType
     * @exception RegistryException if an error occurs
     * @exception FactoryException if an error occurs
     */
    DataType createDataType( String name )
        throws RegistryException, FactoryException;
}
