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
import org.apache.myrmidon.components.configurer.test.MyRole1;

/**
 * A test class with multiple setters/adders/creators for a property.
 *
 * @author Adam Murdoch
 */
public class ConfigTestIgnoreStringMethods
{
    private MyRole1 m_prop1;
    private ArrayList m_prop2 = new ArrayList();
    private int m_content;
    private ArrayList m_typed = new ArrayList();

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
        if( m_content != test.m_content )
        {
            return false;
        }
        if( !m_typed.equals( test.m_typed ) )
        {
            return false;
        }
        return true;
    }

    //
    // Multiple Setters
    //

    public void setProp1( final String value )
    {
        throw new AssertionFailedError();
    }

    public void setProp1( final MyRole1 value )
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

    //
    // Multiple typed adders
    //

    public void add( final String value )
    {
        throw new AssertionFailedError();
    }

    public void add( final MyRole1 value )
    {
        m_typed.add( value );
    }

    //
    // Multiple content setters
    //

    public void addContent( final int value )
    {
        m_content = value;
    }

    public void addContent( final String value )
    {
        throw new AssertionFailedError();
    }

}
