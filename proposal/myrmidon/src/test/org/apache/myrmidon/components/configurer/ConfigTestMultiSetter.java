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

/**
 * A test class with multiple setters/adders/creators for a property.
 *
 * @author Adam Murdoch
 */
public class ConfigTestMultiSetter
{
    private ConfigTestStringProps m_prop1;
    private ConfigTestStringProps m_prop2;
    private ArrayList m_prop3 = new ArrayList();

    public boolean equals( Object obj )
    {
        ConfigTestMultiSetter test = (ConfigTestMultiSetter)obj;
        if( !DefaultConfigurerTest.equals( m_prop1, test.m_prop1 ) )
        {
            return false;
        }
        if( !DefaultConfigurerTest.equals( m_prop2, test.m_prop2 ) )
        {
            return false;
        }
        if( !m_prop3.equals( test.m_prop3 ) )
        {
            return false;
        }
        return true;
    }

    //
    // Multiple setters
    //

    public void setProp1( final String value )
    {
        throw new AssertionFailedError();
    }

    public void setProp1( final ConfigTestStringProps value )
    {
        m_prop1 = value;
    }

    //
    // Setter and Adder
    //

    public void addProp2( final String value )
    {
        throw new AssertionFailedError();
    }

    public void setProp2( final ConfigTestStringProps value )
    {
        m_prop2 = value;
    }

    //
    // Multiple Adders
    //

    public void addProp3( final String value )
    {
        throw new AssertionFailedError();
    }

    public void addProp3( final ConfigTestStringProps value )
    {
        m_prop3.add( value );
    }
}
