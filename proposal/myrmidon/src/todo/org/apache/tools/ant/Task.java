/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import org.apache.myrmidon.api.TaskException;

public abstract class Task
    extends ProjectComponent
    implements org.apache.myrmidon.api.Task
{
    /**
     * Called by the project to let the task do it's work. This method may be
     * called more than once, if the task is invoked more than once. For
     * example, if target1 and target2 both depend on target3, then running "ant
     * target1 target2" will run all tasks in target3 twice.
     *
     * @throws TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
    }

    /**
     * Log a mesage with the give priority.
     *
     * @param msgLevel the message priority at which this message is to be
     *      logged.
     * @param msg Description of Parameter
     */
    public void log( String msg, int msgLevel )
    {
        project.log( this, msg, msgLevel );
    }

    protected void handleErrorOutput( String line )
    {
        log( line, Project.MSG_ERR );
    }

    protected void handleOutput( String line )
    {
        log( line, Project.MSG_INFO );
    }
}

