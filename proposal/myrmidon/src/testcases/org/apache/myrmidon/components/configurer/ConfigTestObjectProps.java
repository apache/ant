/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple test class.
 *
 * @author Adam Murdoch
 */
public class ConfigTestObjectProps
{
    ConfigTestStringProps m_prop;
    List m_propList = new ArrayList();

    public boolean equals( Object obj )
    {
        ConfigTestObjectProps test = (ConfigTestObjectProps)obj;
        if( !DefaultConfigurerTest.equals( m_prop, test.m_prop ) )
        {
            return false;
        }
        if( !m_propList.equals( test.m_propList ) )
        {
            return false;
        }
        return true;
    }

    public void setProp( final ConfigTestStringProps test )
    {
        m_prop = test;
    }

    public void addAnotherProp( final ConfigTestStringProps test )
    {
        m_propList.add( test );
    }
}
