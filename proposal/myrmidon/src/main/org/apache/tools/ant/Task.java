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
    protected Target target;
    protected String description;
    protected String taskType;
    protected RuntimeConfigurable wrapper;

    /**
     * Sets a description of the current action. It will be usefull in
     * commenting what we are doing.
     *
     * @param desc The new Description value
     */
    public void setDescription( String desc )
    {
        description = desc;
    }

    /**
     * Sets the target object of this task.
     *
     * @param target Target in whose scope this task belongs.
     */
    public void setOwningTarget( Target target )
    {
        this.target = target;
    }

    public String getDescription()
    {
        return description;
    }

    /**
     * Get the Target to which this task belongs
     *
     * @return the task's target.
     */
    public Target getOwningTarget()
    {
        return target;
    }

    /**
     * Returns the wrapper class for runtime configuration.
     *
     * @return The RuntimeConfigurableWrapper value
     */
    public RuntimeConfigurable getRuntimeConfigurableWrapper()
    {
        if( wrapper == null )
        {
            wrapper = new RuntimeConfigurable( this, getName() );
        }
        return wrapper;
    }

    /**
     * Perform this task
     */
    public final void perform()
        throws TaskException
    {
        try
        {
            project.fireTaskStarted( this );
            maybeConfigure();
            execute();
            project.fireTaskFinished( this, null );
        }
        catch( TaskException te )
        {
            project.fireTaskFinished( this, te );
            throw te;
        }
        catch( RuntimeException re )
        {
            project.fireTaskFinished( this, re );
            throw re;
        }
    }

    /**
     * Called by the project to let the task do it's work. This method may be
     * called more than once, if the task is invoked more than once. For
     * example, if target1 and target2 both depend on target3, then running "ant
     * target1 target2" will run all tasks in target3 twice.
     *
     * @throws BuildException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
    }

    /**
     * Called by the project to let the task initialize properly.
     *
     * @throws BuildException if someting goes wrong with the build
     */
    public void init()
        throws TaskException
    {
    }

    /**
     * Log a message with the default (INFO) priority.
     *
     * @param msg Description of Parameter
     */
    public void log( String msg )
    {
        log( msg, Project.MSG_INFO );
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

    /**
     * Configure this task - if it hasn't been done already.
     *
     * @exception BuildException Description of Exception
     */
    public void maybeConfigure()
        throws TaskException
    {
        if( wrapper != null )
        {
            wrapper.maybeConfigure( project );
        }
    }

    protected void setRuntimeConfigurableWrapper( RuntimeConfigurable wrapper )
    {
        this.wrapper = wrapper;
    }

    protected void handleErrorOutput( String line )
    {
        log( line, Project.MSG_ERR );
    }

    protected void handleOutput( String line )
    {
        log( line, Project.MSG_INFO );
    }

    /**
     * Set the name with which the task has been invoked.
     *
     * @param type the name the task has been invoked as.
     */
    void setTaskType( String type )
    {
        this.taskType = type;
    }
}

