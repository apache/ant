/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.data;

import java.util.List;
import java.util.ArrayList;
import org.apache.myrmidon.components.configurer.DefaultConfigurerTest;

/**
 * Simple class to test typed adder.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ConfigTestPropResolution
{
    private String m_prop;

    public boolean equals( final Object obj )
    {
        final ConfigTestPropResolution test = (ConfigTestPropResolution)obj;
        if( !DefaultConfigurerTest.equals( m_prop, test.m_prop ) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void setProp( final String value )
    {
        m_prop = value;
    }
}
