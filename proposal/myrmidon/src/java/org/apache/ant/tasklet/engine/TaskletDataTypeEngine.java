/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import org.apache.ant.datatypes.DefaultDataTypeEngine;
import org.apache.avalon.camelot.DefaultFactory;

public class TaskletDataTypeEngine
    extends DefaultDataTypeEngine
{
    /**
     * Set the DataTypeFactory.
     * Package access intended.
     */
    void setFactory( final DefaultFactory factory )
    {
        m_factory = factory;
    }

    protected DefaultFactory createFactory()
    {
        return m_factory;
    }
}
