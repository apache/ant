/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import org.apache.avalon.framework.configuration.Configuration;

/**
 * A service definition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class ServiceDefinition
{
    private final String m_roleShorthand;
    private final String m_factoryClass;
    private final Configuration m_config;

    public ServiceDefinition( final String roleShorthand,
                              final String factoryClass,
                              final Configuration config )
    {
        m_roleShorthand = roleShorthand;
        m_factoryClass = factoryClass;
        m_config = config;
    }

    /**
     * Returns the role that the service implements.
     */
    public String getRoleShorthand()
    {
        return m_roleShorthand;
    }

    /**
     * Returns the name of the factory class for creating the service.
     */
    public String getFactoryClass()
    {
        return m_factoryClass;
    }

    /**
     * Returns the service configuration.
     */
    public Configuration getConfig()
    {
        return m_config;
    }
}
