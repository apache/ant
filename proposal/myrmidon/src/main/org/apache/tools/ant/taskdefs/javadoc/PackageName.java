/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.javadoc;

public class PackageName
{
    private String m_name;

    public void setName( final String name )
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    public String toString()
    {
        return getName();
    }
}
