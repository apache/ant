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
import org.apache.ant.util.Condition;
import org.apache.avalon.framework.configuration.Configuration;

/**
 * Default implementation of target.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTarget
    implements Target
{
    protected final ArrayList   m_dependencies     = new ArrayList();
    protected final ArrayList   m_tasks            = new ArrayList();
    protected final Condition   m_condition;

    /**
     * Constructor taking condition for target.
     *
     * @param condition the condition
     */
    public DefaultTarget( final Condition condition )
    {
        m_condition = condition;
    }

    /**
     * Constructor for target with no condition.
     */
    public DefaultTarget()
    {
        this( null );
    }

    /**
     * Get condition under which target is executed.
     *
     * @return the condition for target or null
     */
    public Condition getCondition()
    {
        return m_condition;
    }
    /**
     * Get dependencies of target
     *
     * @return the dependency list
     */
    public Iterator getDependencies()
    {
        return m_dependencies.iterator();
    }

    /**
     * Get tasks in target
     *
     * @return the target list
     */
    public Iterator getTasks()
    {
        return m_tasks.iterator();
    }

    /**
     * Add a dependency to target.
     *
     * @param dependency the dependency
     */
    public void addDependency( final String dependency )
    {
        m_dependencies.add( dependency );
    }

    /**
     * Add task to target.
     *
     * @param taskConfiguration the task representation
     */
    public void addTask( final Configuration taskConfiguration )
    {
        m_tasks.add( taskConfiguration );
    }
}
