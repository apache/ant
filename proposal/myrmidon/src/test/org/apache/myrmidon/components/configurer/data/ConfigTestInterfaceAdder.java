/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer.data;

import java.util.ArrayList;
import org.apache.myrmidon.components.configurer.MyRole1;

/**
 * A test class with an interface property.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class ConfigTestInterfaceAdder
{
    private final ArrayList m_elems = new ArrayList();

    public void addPropA( final MyRole1 role1 )
    {
        m_elems.add( role1 );
    }

    public boolean equals( Object obj )
    {
        final ConfigTestInterfaceAdder test = (ConfigTestInterfaceAdder)obj;
        return m_elems.equals( test.m_elems );
    }
}
