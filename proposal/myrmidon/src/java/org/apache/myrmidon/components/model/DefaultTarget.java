/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.model;

import java.util.ArrayList;
import org.apache.avalon.framework.configuration.Configuration;

/**
 * Default implementation of target.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTarget
    implements Target
{
    private final ArrayList   m_dependencies     = new ArrayList();
    private final ArrayList   m_tasks            = new ArrayList();
    private final Condition   m_condition;

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
    public String[] getDependencies()
    {
        return (String[])m_dependencies.toArray( new String[ 0 ] );
    }

    /**
     * Get tasks in target
     *
     * @return the target list
     */
    public Configuration[] getTasks()
    {
        return (Configuration[])m_tasks.toArray( new Configuration[ 0 ] );
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
    public void addTask( final Configuration taskData )
    {
        m_tasks.add( taskData );
    }
}
