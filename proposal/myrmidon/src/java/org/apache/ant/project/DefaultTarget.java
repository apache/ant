/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.ant.configuration.Configuration;

public class DefaultTarget
    implements Target
{
    protected ArrayList         m_dependencies     = new ArrayList();
    protected ArrayList         m_tasks            = new ArrayList();
    protected String            m_condition;
    protected boolean           m_isIfCondition;

    public Iterator getDependencies()
    {
        return m_dependencies.iterator();
    }

    public Iterator getTasks()
    {
        return m_tasks.iterator();
    }

    public String getCondition()
    {
        return m_condition;
    }

    public void setCondition( final String condition )
    {
        m_condition = condition;
    }

    public boolean isIfCondition()
    {
        return m_isIfCondition;
    }

    public void setIfCondition( final boolean isIfCondition )
    {
        m_isIfCondition = isIfCondition;
    }

    public void addDependency( final String dependency )
    {
        m_dependencies.add( dependency );
    }

    public void addTask( final Configuration taskConfiguration )
    {
        m_tasks.add( taskConfiguration );
    }
}
