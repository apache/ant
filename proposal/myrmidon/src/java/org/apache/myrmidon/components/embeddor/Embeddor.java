/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.embeddor;

import java.util.Map;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.myrmidon.components.model.Project;
import org.apache.myrmidon.components.manager.ProjectManager;

/**
 * Interface through which you embed Myrmidon into applications.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Embeddor
    extends Component, Parameterizable, Initializable, Startable, Disposable
{
    String ROLE = "org.apache.myrmidon.components.embeddor.Embeddor";

    /**
     * Create a project.
     *
     * @return the created Project
     */
    Project createProject( String location, String type, Parameters parameters )
        throws Exception;

    /**
     * Create a ProjectManager for a particular project.
     *
     * @param project the root project
     * @param defines the defines in project
     * @return the ProjectManager
     */
    ProjectManager createProjectManager( Project project, Parameters parameters )
        throws Exception;
}
