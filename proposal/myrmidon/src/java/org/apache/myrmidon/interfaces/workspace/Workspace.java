/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.workspace;

import org.apache.avalon.framework.component.Component;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * This is the abstraction through which Projects are managed and executed.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface Workspace
    extends Component
{
    String ROLE = Workspace.class.getName();

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
     * Execute a target in a particular project.
     *
     * @param project the Project
     * @param target the name of the target
     * @param defines the defines
     * @exception TaskException if an error occurs
     */
    void executeProject( Project project, String target )
        throws TaskException;
}
