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
import org.apache.myrmidon.framework.DataType;

/**
 * Simple class to test typed adder.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ConfigTestTypedAdderConversion
    implements DataType
{
    private ArrayList m_roles = new ArrayList();
    private String m_prop;

    public void setProp( final String prop )
    {
        m_prop = prop;
    }
    public void add( final MyRole1 role1 )
    {
        m_roles.add( role1 );
    }

    public boolean equals( final Object object )
    {
        final ConfigTestTypedAdderConversion other = (ConfigTestTypedAdderConversion)object;
        return m_roles.equals( other.m_roles );
    }
}
