/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.model;

import java.util.ArrayList;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.framework.Condition;

/**
 * Targets in build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Target
{
    private final ArrayList m_dependencies = new ArrayList();
    private final ArrayList m_tasks = new ArrayList();
    private final Condition m_condition;

    /**
     * Constructor taking condition for target.
     *
     * @param condition the condition
     */
    public Target( final Condition condition,
                   final Configuration[] tasks,
                   final String[] dependencies )
    {
        m_condition = condition;

        for( int i = 0; i < tasks.length; i++ )
        {
            m_tasks.add( tasks[ i ] );
        }

        if( null != dependencies )
        {
            for( int i = 0; i < dependencies.length; i++ )
            {
                m_dependencies.add( dependencies[ i ] );
            }
        }
    }

    /**
     * Get condition under which target is executed.
     *
     * @return the condition for target or null
     */
    public final Condition getCondition()
    {
        return m_condition;
    }

    /**
     * Get dependencies of target
     *
     * @return the dependency list
     */
    public final String[] getDependencies()
    {
        return (String[])m_dependencies.toArray( new String[ 0 ] );
    }

    /**
     * Get tasks in target
     *
     * @return the target list
     */
    public final Configuration[] getTasks()
    {
        return (Configuration[])m_tasks.toArray( new Configuration[ 0 ] );
    }
}
