/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import org.apache.ant.AntException;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.avalon.Component;
import org.apache.log.Logger;

public interface ProjectEngine
    extends Component
{
    void setLogger( Logger logger );

    TaskletEngine getTaskletEngine();

    void addProjectListener( ProjectListener listener );
    void removeProjectListener( ProjectListener listener );

    void execute( Project project, String target )
        throws AntException;

    void execute( Project project, String target, TaskletContext context )
        throws AntException;
}
