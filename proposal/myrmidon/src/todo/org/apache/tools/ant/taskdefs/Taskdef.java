/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildException;

/**
 * Define a new task.
 *
 * @author <a href="stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Taskdef extends Definer
{
    protected void addDefinition( String name, Class c )
        throws BuildException
    {
        project.addTaskDefinition( name, c );
    }
}
