/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test.data;

import java.util.ArrayList;
import org.apache.avalon.framework.configuration.Configuration;

/**
 * Simple class to test adder for Configurations.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ConfigTestConfigAdder
{
    private ArrayList m_configurations = new ArrayList();

    public void addConfig( final Configuration configuration )
    {
        m_configurations.add( configuration );
    }

    public boolean equals( final Object object )
    {
        final ConfigTestConfigAdder other = (ConfigTestConfigAdder)object;
        return m_configurations.equals( other.m_configurations );
    }
}
