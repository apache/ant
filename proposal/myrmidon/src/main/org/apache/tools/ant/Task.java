/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.util.FileUtils;

public abstract class Task
    extends ProjectComponent
    implements org.apache.myrmidon.api.Task
{
    protected Target target;
    protected String description;
    protected Location location = Location.UNKNOWN_LOCATION;
    protected String taskName;
    protected String taskType;
    private boolean invalid;
    protected RuntimeConfigurable wrapper;

    private UnknownElement replacement;

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
     * Sets the file location where this task was defined.
     *
     * @param location The new Location value
     */
    public void setLocation( Location location )
    {
        this.location = location;
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

    /**
     * Set the name to use in logging messages.
     *
     * @param name the name to use in logging messages.
     */
    public void setTaskName( String name )
    {
        this.taskName = name;
    }

    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the file location where this task was defined.
     *
     * @return The Location value
     */
    public Location getLocation()
    {
        return location;
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
            wrapper = new RuntimeConfigurable( this, getTaskName() );
        }
        return wrapper;
    }

    /**
     * Get the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName()
    {
        return taskName;
    }

    /**
     * Perform this task
     */
    public final void perform()
        throws TaskException
    {
        if( !invalid )
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
                if( te instanceof BuildException )
                {
                    BuildException be = (BuildException)te;
                    if( be.getLocation() == Location.UNKNOWN_LOCATION )
                    {
                        be.setLocation( getLocation() );
                    }
                }
                project.fireTaskFinished( this, te );
                throw te;
            }
            catch( RuntimeException re )
            {
                project.fireTaskFinished( this, re );
                throw re;
            }
        }
        else
        {
            UnknownElement ue = getReplacement();
            Task task = ue.getTask();
            task.perform();
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
        if( !invalid )
        {
            if( wrapper != null )
            {
                wrapper.maybeConfigure( project );
            }
        }
        else
        {
            getReplacement();
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

    /**
     * Mark this task as invalid.
     */
    final void markInvalid()
    {
        invalid = true;
    }

    /**
     * Create an UnknownElement that can be used to replace this task.
     *
     * @return The Replacement value
     */
    private UnknownElement getReplacement()
        throws TaskException
    {
        if( replacement == null )
        {
            replacement = new UnknownElement( taskType );
            replacement.setProject( project );
            replacement.setTaskType( taskType );
            replacement.setTaskName( taskName );
            replacement.setLocation( location );
            replacement.setOwningTarget( target );
            replacement.setRuntimeConfigurableWrapper( wrapper );
            wrapper.setProxy( replacement );
            target.replaceChild( this, replacement );
            replacement.maybeConfigure();
        }
        return replacement;
    }
}

