/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.data;

import java.util.ArrayList;
import java.util.List;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.components.configurer.DefaultConfigurerTest;

/**
 * A simple test class with string properties.
 *
 * @author Adam Murdoch
 */
public class ConfigTestIdResolve
    implements DataType
{
    private String m_someProp;

    public boolean equals( final Object obj )
    {
        final ConfigTestIdResolve test = (ConfigTestIdResolve)obj;
        if( !DefaultConfigurerTest.equals( m_someProp, test.m_someProp ) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void setSomeProp( final String value )
    {
        m_someProp = value;
    }
}
