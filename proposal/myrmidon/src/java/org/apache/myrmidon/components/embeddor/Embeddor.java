/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.embeddor;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.myrmidon.components.builder.ProjectBuilder;
import org.apache.myrmidon.components.manager.ProjectManager;

/**
 * Interface through which you embed Myrmidon into applications.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Embeddor
    extends Component, Parameterizable, Initializable, Startable, Disposable
{
    /**
     * Retrieve builder for runtime.
     * Valid after initialize() call
     *
     * @return the ProjectBuilder
     */
    ProjectBuilder getProjectBuilder();

    /**
     * Retrieve project engine for runtime.
     * Valid after initialize() call
     *
     * @return the ProjectBuilder
     */
    ProjectManager getProjectManager();
}
