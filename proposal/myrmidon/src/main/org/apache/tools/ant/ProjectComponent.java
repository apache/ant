/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import org.apache.myrmidon.api.AbstractTask;

/**
 * Base class for components of a project, including tasks and data types.
 * Provides common facilities.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */

public abstract class ProjectComponent
    extends AbstractTask
{
    protected Project project = null;

    /**
     * Sets the project object of this component. This method is used by project
     * when a component is added to it so that the component has access to the
     * functions of the project. It should not be used for any other purpose.
     *
     * @param project Project in whose scope this component belongs.
     */
    public void setProject( Project project )
    {
        this.project = project;
    }

    /**
     * Get the Project to which this component belongs
     *
     * @return the components's project.
     */
    public Project getProject()
    {
        return project;
    }

    /**
     * Log a message with the default (INFO) priority.
     *
     * @param msg Description of Parameter
     */
    public void log( String msg )
    {
        log( msg, Project.MSG_INFO );
    }

    /**
     * Log a mesage with the give priority.
     *
     * @param msgLevel the message priority at which this message is to be
     *      logged.
     * @param msg Description of Parameter
     */
    public void log( String msg, int msgLevel )
    {
        if( project != null )
        {
            project.log( msg, msgLevel );
        }
    }
}

