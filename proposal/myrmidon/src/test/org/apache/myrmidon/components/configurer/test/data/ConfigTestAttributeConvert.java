/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.test.data;

import org.apache.myrmidon.components.AbstractComponentTest;

/**
 * A class for testing conversion.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class ConfigTestAttributeConvert
{
    private int m_intProp;
    private Integer m_integerProp;

    public void setIntProp( final int intProp )
    {
        m_intProp = intProp;
    }

    public void setIntegerProp( final Integer integerProp )
    {
        m_integerProp = integerProp;
    }

    public boolean equals( Object obj )
    {
        ConfigTestAttributeConvert test = (ConfigTestAttributeConvert)obj;
        if( m_intProp != test.m_intProp )
        {
            return false;
        }
        if( !AbstractComponentTest.equals( m_integerProp, test.m_integerProp ) )
        {
            return false;
        }

        return true;
    }
}
