/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.model.Project;

/**
 * A task which executes a target in the current project,
 * or a referenced project.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 * @ant.task name="ant-call"
 */
public class AntCallTask
    extends AbstractAntTask
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( AntCallTask.class );

    private String m_project;

    /**
     * Specifies the project to execute. If not specified, the current
     * project is used.
     * @param project the name of the Project to execute.
     */
    public void setProject( String project )
    {
        m_project = project;
    }

    /**
     * Get/create/build the project which will be executed.
     * Subclasses will override this method to provide different means
     * of obtaining a project to execute.
     * @return The project containing the target to execute.
     * @throws Exception If a problem occurred building the project.
     */
    protected Project getProject() throws Exception
    {
        Project currentProject =
            (Project)getContext().getService( Project.class );

        // By default, use the current project.
        Project referencedProject = currentProject;

        if( m_project != null )
        {
            referencedProject = currentProject.getProject( m_project );
            if( referencedProject == null )
            {
                final String message =
                    REZ.getString( "antcall.invalid-project.error" );
                throw new TaskException( message );
            }
        }

        return referencedProject;
    }
}