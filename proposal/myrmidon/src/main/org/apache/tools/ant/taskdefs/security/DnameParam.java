/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.security;

public final class DnameParam
{
    private String m_name;
    private String m_value;

    public void setName( final String name )
    {
        m_name = name;
    }

    public void setValue( final String value )
    {
        m_value = value;
    }

    public String getName()
    {
        return m_name;
    }

    public String getValue()
    {
        return m_value;
    }
}
