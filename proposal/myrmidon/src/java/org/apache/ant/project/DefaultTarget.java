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
import org.apache.ant.datatypes.Condition;

public class DefaultTarget
    implements Target
{
    protected final ArrayList   m_dependencies     = new ArrayList();
    protected final ArrayList   m_tasks            = new ArrayList();
    protected final Condition   m_condition;

    public DefaultTarget( final Condition condition )
    {
        m_condition = condition;
    }

    public DefaultTarget()
    {
        this( null );
    }

    public Condition getCondition()
    {
        return m_condition;
    }
    
    public Iterator getDependencies()
    {
        return m_dependencies.iterator();
    }

    public Iterator getTasks()
    {
        return m_tasks.iterator();
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
