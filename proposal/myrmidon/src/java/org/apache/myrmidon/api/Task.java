/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api;

/**
 * This is the interface that tasks implement to be executed in Myrmidon runtime.
 *
 * Instances can also implement the Avalon LogEnabled method to receive a logger.
 *
 * Tasks can also choose to implement Avalon Configurable if they wish to directly
 * receive the Configuration data representing the task. If this interface is
 * not implemented then the container will be responsble for mapping configuration
 * onto the task object.
 *
 * The Components passed in via ComponentManager are determined by container.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:role shorthand="task"
 */
public interface Task
{
    /**
     * Specify the context in which the task operates in.
     * The Task will use the TaskContext to receive information
     * about it's environment.
     */
    void contextualize( TaskContext context )
        throws TaskException;

    /**
     * Execute task.
     * This method is called to perform actual work associated with task.
     * It is called after Task has been Configured.
     *
     * @exception TaskException if task fails to execute
     */
    void execute()
        throws TaskException;
}
