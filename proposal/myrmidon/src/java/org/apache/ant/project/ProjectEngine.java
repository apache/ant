/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import org.apache.ant.AntException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.avalon.framework.component.Component;
import org.apache.myrmidon.model.Project;

/**
 * This is the interface between ProjectEngine and rest of the system.
 * This is the interface that tasks/frontends must use to interact with 
 * project execution.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface ProjectEngine
    extends Component
{
    /**
     * Add a listener to project events.
     *
     * @param listener the listener
     */
    void addProjectListener( ProjectListener listener );

    /**
     * Remove a listener from project events.
     *
     * @param listener the listener
     */
    void removeProjectListener( ProjectListener listener );

    /**
     * Execute a target in a particular project, in a particular context.
     *
     * @param project the Project
     * @param target the name of the target
     * @param context the context
     * @exception AntException if an error occurs
     */
    void executeTarget( Project project, String target, TaskContext context )
        throws AntException;
}
