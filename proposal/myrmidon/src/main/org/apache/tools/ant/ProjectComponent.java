/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Base class for components of a project, including tasks and data types.
 * Provides common facilities.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */

public abstract class ProjectComponent
    extends AbstractTask
{
    private Project project;

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

    public void execute()
        throws TaskException
    {
        //HACK: NOOP execute - should be deleted in the future!
    }

    public void log( final String message, int priority )
    {
        switch( priority )
        {
            case Project.MSG_ERR:
                getLogger().error( message );
                break;
            case Project.MSG_WARN:
                getLogger().warn( message );
                break;
            case Project.MSG_INFO:
                getLogger().info( message );
                break;
            case Project.MSG_VERBOSE:
                getLogger().debug( message );
                break;
            case Project.MSG_DEBUG:
                getLogger().debug( message );
                break;

            default:
                getLogger().debug( message );
        }
    }
}

