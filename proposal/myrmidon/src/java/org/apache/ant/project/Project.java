/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.Component;

/**
 * Interface through which to interact with projects.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface Project
    extends Component
{
    // the name of currently executing project
    String PROJECT          = "ant.project.name"; 

    // the name of currently executing project
    String PROJECT_FILE     = "ant.project.file"; 

    // the name of currently executing target
    String TARGET           = "ant.target.name"; 

    /**
     * Get name of default target.
     *
     * @return the default target name
     */
    String getDefaultTargetName();

    /**
     * Retrieve implicit target. 
     * The implicit target is top level tasks. 
     * Currently restricted to property tasks.
     *
     * @return the Target
     */
    Target getImplicitTarget();

    /**
     * Retrieve a target by name.
     *
     * @param name the name of target
     * @return the Target or null if no target exists with name
     */
    Target getTarget( String name );

    /**
     * Retrieve names of all targets in project.
     *
     * @return the iterator of project names
     */
    Iterator getTargetNames();

    /**
     * Get project (top-level) context.
     *
     * @return the context
     */
    TaskletContext getContext();
}
