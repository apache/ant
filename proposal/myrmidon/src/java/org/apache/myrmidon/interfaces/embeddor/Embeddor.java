/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.embeddor;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * Interface through which you embed Myrmidon into applications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface Embeddor
    extends Parameterizable, Initializable, Startable, Disposable
{
    String ROLE = Embeddor.class.getName();

    /**
     * Creates a project from a project file.
     *
     * @param location The path to the project file.
     * @param type The project file type.  If null the type is inferred from the
     *             project file name.
     * @param parameters The project builder parameters.
     * @return the created Project
     * @todo Should location be a URL or will it automatically assume file
     *       unless there is a protocol section like ftp:, file: etc
     * @todo parameters needs more thought put into it.
     */
    Project createProject( String location, String type, Parameters parameters )
        throws Exception;

    /**
     * Creates a project listener.
     *
     * @param name The shorthand name of the listener.
     * @return the listener.
     */
    ProjectListener createListener( String name )
        throws Exception;

    /**
     * Creates a {@link Workspace} that can be used to execute projects.
     *
     * @param parameters The properties to define in the workspace
     * @return the Workspace
     */
    Workspace createWorkspace( Parameters parameters )
        throws Exception;
}
