/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test.data;

import java.util.ArrayList;
import org.apache.myrmidon.AbstractMyrmidonTest;

/**
 * A test class with a setter and adder with the same property name.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class ConfigTestSetAndAdd
{
    private String m_prop;
    private ArrayList m_nested = new ArrayList();

    public void setProp( final String prop )
    {
        m_prop = prop;
    }

    public void addProp( final ConfigTestSetAndAdd elem )
    {
        m_nested.add( elem );
    }

    public boolean equals( final Object obj )
    {
        ConfigTestSetAndAdd test = (ConfigTestSetAndAdd)obj;
        if( ! AbstractMyrmidonTest.equals( m_prop, test.m_prop) )
        {
            return false;
        }
        else if( ! m_nested.equals( test.m_nested ) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}
