/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant;

import java.util.Properties;
import org.apache.ant.project.ProjectBuilder;
import org.apache.ant.project.ProjectEngine;
import org.apache.avalon.Component;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;

/**
 * Interface to the Ant runtime.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface AntEngine
    extends Component, Initializable, Disposable
{
    /**
     * Setup basic properties of engine. 
     * Called before init() and can be used to specify alternate components in system.
     *
     * @param properties the properties
     */
    void setProperties( Properties properties );

    /**
     * Retrieve builder for runtime.
     * Valid after init() call
     *
     * @return the ProjectBuilder
     */
    ProjectBuilder getProjectBuilder();
    
    /**
     * Retrieve project engine for runtime.
     * Valid after init() call
     *
     * @return the ProjectBuilder
     */
    ProjectEngine getProjectEngine();
}
