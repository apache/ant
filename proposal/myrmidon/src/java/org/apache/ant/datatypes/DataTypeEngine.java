/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.datatypes;

import org.apache.avalon.Component;
import org.apache.avalon.Loggable;
import org.apache.avalon.camelot.FactoryException;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.avalon.camelot.RegistryException;

public interface DataTypeEngine
    extends Component, Loggable
{
    LocatorRegistry getRegistry();

    DataType createDataType( String name )
        throws RegistryException, FactoryException;
}
