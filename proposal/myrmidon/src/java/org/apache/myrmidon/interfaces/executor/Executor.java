/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.executor;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.myrmidon.api.TaskException;

/**
 * Engine inteface that should be implemented by all tasklet engines.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface Executor
    extends Component
{
    String ROLE = Executor.class.getName();

    /**
     * execute a task.
     *
     * @param task the configruation data for task
     * @exception TaskException if an error occurs
     */
    void execute( Configuration task, ExecutionFrame frame )
        throws TaskException;
}
