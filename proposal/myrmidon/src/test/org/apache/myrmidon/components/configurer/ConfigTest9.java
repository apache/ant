/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * Simple class to test adder for Configurations.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ConfigTest9
    implements Configurable
{
    private Configuration m_configuration;

    public void configure( Configuration configuration )
        throws ConfigurationException
    {
        m_configuration = configuration;
    }

    public boolean equals( final Object object )
    {
        final ConfigTest9 other = (ConfigTest9)object;
        return m_configuration == other.m_configuration;
    }
}
