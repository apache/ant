/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.util.Enumeration;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

/**
 * Implements a single threaded task execution. <p>
 *
 *
 *
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 */
public class Sequential extends Task
    implements TaskContainer
{

    /**
     * Optional Vector holding the nested tasks
     */
    private Vector nestedTasks = new Vector();

    /**
     * Add a nested task to Sequential. <p>
     *
     *
     *
     * @param nestedTask Nested task to execute Sequential <p>
     *
     *
     */
    public void addTask( Task nestedTask )
    {
        nestedTasks.addElement( nestedTask );
    }

    /**
     * Execute all nestedTasks.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        for( Enumeration e = nestedTasks.elements(); e.hasMoreElements(); )
        {
            Task nestedTask = (Task)e.nextElement();
            nestedTask.perform();
        }
    }
}
