/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.interfaces.model;

import java.io.File;
import org.apache.avalon.framework.component.Component;

/**
 * Abstraction used to interact with projects.
 * Implementations may choose to structure it anyway they choose.
 *
 * TODO: Determine if projects should carry their own name. Breaks IOC but
 * Can be useful as project files embed own name (or should that be description).
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface Project
    extends Component
{
    String ROLE = "org.apache.myrmidon.interfaces.model.Project";

    // the name of currently executing project
    String PROJECT = "myrmidon.project.name";

    // the name of currently executing project
    //String PROJECT_FILE     = "myrmidon.project.file";

    // the name of currently executing target
    //String TARGET           = "myrmidon.target.name";

    /**
     * Get the imports for project.
     *
     * @return the imports
     */
    TypeLib[] getTypeLibs();

    /**
     * Get names of projects referred to by this project.
     *
     * @return the names
     */
    String[] getProjectNames();

    /**
     * Retrieve project reffered to by this project.
     *
     * @param name the project name
     * @return the Project or null if none by that name
     */
    Project getProject( String name );

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
    String[] getTargetNames();

    /**
     * Retrieve base directory of project.
     *
     * @return the projects base directory
     */
    File getBaseDirectory();
}
