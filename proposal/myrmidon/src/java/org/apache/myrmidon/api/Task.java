/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.api;

import org.apache.avalon.framework.component.Component;

/**
 * This is the interface that tasks implement to be executed in Myrmidon runtime.
 * 
 * Instances can also implement the Avalon lifecycle methods 
 * Loggable, Contextualizable, Composable, Initializable and Disposable.
 * Each of these lifecycle stages will be executed at appropriate time.
 *
 * Tasks can also choose to implement Configurable if they wish to directly
 * receive the Configuration data representing the task. If this interface is
 * not implemented then the engine will be responsbil for mapping configuration
 * to task object.
 *
 * The Components passed in via ComponentManager are determined by container.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Task
    extends Component
{
    String ROLE = "org.apache.myrmidon.api.Task";

    /**
     * Execute task. 
     * This method is called to perform actual work associated with task.
     * It is called after Task has been Configured and Initialized and before
     * being Disposed (If task implements appropriate interfaces).
     *
     * @exception TaskException if an error occurs
     */
    void execute()
        throws TaskException;
}
