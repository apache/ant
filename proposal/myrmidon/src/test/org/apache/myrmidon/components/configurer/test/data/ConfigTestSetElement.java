/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test.data;

import java.util.ArrayList;
import java.util.List;
import org.apache.myrmidon.components.configurer.test.DefaultConfigurerTestCase;

/**
 * A simple test class.
 *
 * @author Adam Murdoch
 */
public class ConfigTestSetElement
{
    private List m_propList = new ArrayList();
    private String m_someProp;

    public boolean equals( Object obj )
    {
        ConfigTestSetElement test = (ConfigTestSetElement)obj;
        if( !m_propList.equals( test.m_propList ) )
        {
            return false;
        }
        else if( !DefaultConfigurerTestCase.equals( m_someProp, test.m_someProp ) )
        {
            return false;
        }
        return true;
    }

    public void setSomeProp( final String value )
    {
        m_someProp = value;
    }

    public void addProp( final ConfigTestSetElement test )
    {
        m_propList.add( test );
    }
}
