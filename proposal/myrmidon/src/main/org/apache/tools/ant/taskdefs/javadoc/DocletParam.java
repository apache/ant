/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

public class DocletParam
{
    private String m_name;
    private String m_value;

    public void setName( final String name )
    {
        this.m_name = name;
    }

    public void setValue( final String value )
    {
        this.m_value = value;
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
