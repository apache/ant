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
import org.apache.myrmidon.components.configurer.DefaultConfigurerTest;
import org.apache.myrmidon.components.configurer.MyRole1;
import org.apache.myrmidon.components.configurer.ConfigTestInterfaceProp;

/**
 * A simple test class.
 *
 * @author Adam Murdoch
 */
public class ConfigTestReferenceConversion
{
    private final ArrayList m_elems = new ArrayList();

    public void setPropA( final MyRole1 role1 )
    {
        m_elems.add( role1 );
    }

    public boolean equals( Object obj )
    {
        final ConfigTestReferenceConversion test = (ConfigTestReferenceConversion)obj;
        return m_elems.equals( test.m_elems );
    }
}
