/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.property.PropertyStore;

/**
 * Frames in which tasks are executed.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultExecutionFrame
    implements ExecutionFrame
{
    private final Logger m_logger;
    private final PropertyStore m_propertyStore;
    private final ServiceManager m_serviceManager;

    public DefaultExecutionFrame( final Logger logger,
                                  final PropertyStore propertyStore,
                                  final ServiceManager serviceManager )
    {
        m_logger = logger;
        m_propertyStore = propertyStore;
        m_serviceManager = serviceManager;
    }

    /**
     * Returns the logger which is to be supplied to tasks.
     */
    public Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Returns the set of services to use to create, configure, and execute
     * tasks.
     */
    public ServiceManager getServiceManager()
    {
        return m_serviceManager;
    }

    /**
     * Returns the set of properties to be supplied to tasks.
     */
    public PropertyStore getProperties()
    {
        return m_propertyStore;
    }
}
