/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test.data;

import java.util.ArrayList;
import junit.framework.AssertionFailedError;
import org.apache.myrmidon.components.configurer.test.DefaultConfigurerTestCase;

/**
 * A test class with multiple setters/adders/creators for a property.
 *
 * @author Adam Murdoch
 */
public class ConfigTestIgnoreStringMethods
{
    private ConfigTestIgnoreStringMethods m_prop1;
    private ArrayList m_prop2 = new ArrayList();

    public boolean equals( Object obj )
    {
        ConfigTestIgnoreStringMethods test = (ConfigTestIgnoreStringMethods)obj;
        if( !DefaultConfigurerTestCase.equals( m_prop1, test.m_prop1 ) )
        {
            return false;
        }
        if( !m_prop2.equals( test.m_prop2 ) )
        {
            return false;
        }
        return true;
    }

    //
    // Multiple setters
    //

    public void addProp1( final String value )
    {
        throw new AssertionFailedError();
    }

    public void addProp1( final ConfigTestIgnoreStringMethods value )
    {
        m_prop1 = value;
    }

    //
    // Multiple Adders
    //

    public void addProp2( final String value )
    {
        throw new AssertionFailedError();
    }

    public void addProp2( final ConfigTestIgnoreStringMethods value )
    {
        m_prop2.add( value );
    }
}
