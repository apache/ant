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
import org.apache.myrmidon.framework.DataType;

/**
 * A simple test class with string properties.
 *
 * @author Adam Murdoch
 */
public class ConfigTestStringProps
    implements DataType
{
    private String m_someProp;
    private List m_propList = new ArrayList();
    private String m_content;

    public boolean equals( final Object obj )
    {
        final ConfigTestStringProps test = (ConfigTestStringProps)obj;
        if( !DefaultConfigurerTest.equals( m_someProp, test.m_someProp ) )
        {
            return false;
        }
        else if( !m_propList.equals( test.m_propList ) )
        {
            return false;
        }
        else if( !DefaultConfigurerTest.equals( m_content, test.m_content ) )
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

    public void addProp( final String value )
    {
        m_propList.add( value );
    }

    public void addContent( final String content )
    {
        m_content = content;
    }
}
