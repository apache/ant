/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.util.ArrayList;
import org.apache.avalon.framework.configuration.Configuration;

/**
 * This object contains an ordered list of tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class TaskList
{
    private ArrayList m_tasks = new ArrayList();

    public void add( final Configuration task )
    {
        m_tasks.add( task );
    }

    public Configuration[] getTasks()
    {
        return (Configuration[])m_tasks.toArray( new Configuration[ m_tasks.size() ] );
    }
}
