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

/**
 * Targets in build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class Target
{
    private final ArrayList m_dependencies = new ArrayList();
    private final ArrayList m_tasks = new ArrayList();

    /**
     * Constructs a target.
     */
    public Target( final Configuration[] tasks,
                   final Dependency[] dependencies )
    {
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
     * Get dependencies of target
     *
     * @return the dependency list
     */
    public final Dependency[] getDependencies()
    {
        return (Dependency[])m_dependencies.toArray( new Dependency[ 0 ] );
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
