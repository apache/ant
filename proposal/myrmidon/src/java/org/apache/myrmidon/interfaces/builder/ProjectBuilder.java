/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.builder;

import org.apache.myrmidon.interfaces.model.Project;

/**
 * Interface implemented by components that build projects from sources.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:role shorthand="project-builder"
 */
public interface ProjectBuilder
{
    /** Role name for this interface. */
    String ROLE = ProjectBuilder.class.getName();

    /**
     * build a project from source.
     *
     * @param source the project file path.
     * @return the constructed Project
     * @exception ProjectException if an error occurs
     */
    Project build( String source )
        throws ProjectException;
}
