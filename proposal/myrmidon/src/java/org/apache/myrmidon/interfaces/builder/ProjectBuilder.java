/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.builder;

import org.apache.avalon.framework.component.Component;
import org.apache.myrmidon.interfaces.model.Project;

/**
 * Interface implemented by components that build projects from sources.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface ProjectBuilder
    extends Component
{
    String ROLE = "org.apache.myrmidon.interfaces.builder.ProjectBuilder";

    /**
     * build a project from source.
     *
     * @param source the source
     * @return the constructed Project
     * @exception IOException if an error occurs
     * @exception AntException if an error occurs
     */
    Project build( String source )
        throws Exception;
}
