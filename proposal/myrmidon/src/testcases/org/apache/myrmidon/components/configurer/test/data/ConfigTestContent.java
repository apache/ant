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
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.components.configurer.test.DefaultConfigurerTestCase;

/**
 * A simple test class with string properties.
 *
 * @author Adam Murdoch
 */
public class ConfigTestContent
    implements DataType
{
    private String m_content;

    public boolean equals( final Object obj )
    {
        final ConfigTestContent test = (ConfigTestContent)obj;
        if( !DefaultConfigurerTestCase.equals( m_content, test.m_content ) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void addContent( final String content )
    {
        m_content = content;
    }
}
