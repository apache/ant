/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.interfaces.embeddor;

import java.util.Map;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;

/**
 * Interface through which you embed Myrmidon into applications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface Embeddor
    extends Component, Parameterizable, Initializable, Startable, Disposable
{
    String ROLE = "org.apache.myrmidon.interfaces.embeddor.Embeddor";

    /**
     * Create a project.
     *
     * @return the created Project
     */
    Project createProject( String location, String type, Parameters parameters )
        throws Exception;

    /**
     * Create a Workspace for a particular project.
     *
     * @param defines the defines in workspace
     * @return the Workspace
     */
    Workspace createWorkspace( Parameters parameters )
        throws Exception;
}
