/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional;

import org.apache.avalon.framework.logger.AbstractLogEnabled;

public class ClassArgument
    extends AbstractLogEnabled
{
    private String m_name;

    public void setName( String name )
    {
        m_name = name;
        getLogger().info( "ClassArgument.name=" + name );
    }

    public String getName()
    {
        return m_name;
    }
}
