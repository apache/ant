/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant;

import java.util.EventObject;

public class BuildEvent
    extends EventObject
{
    private int priority = Project.MSG_VERBOSE;
    private Throwable exception;
    private String message;
    private Project project;
    private Target target;
    private Task task;

    /**
     * Construct a BuildEvent for a project level event
     *
     * @param project the project that emitted the event.
     */
    public BuildEvent( Project project )
    {
        super( project );
        this.project = project;
        this.target = null;
        this.task = null;
    }

    /**
     * Construct a BuildEvent for a target level event
     *
     * @param target the target that emitted the event.
     */
    public BuildEvent( Target target )
    {
        super( target );
        this.project = target.getProject();
        this.target = target;
        this.task = null;
    }

    /**
     * Construct a BuildEvent for a task level event
     *
     * @param task the task that emitted the event.
     */
    public BuildEvent( Task task )
    {
        super( task );
        this.project = task.getProject();
        this.task = task;
    }

    public void setException( Throwable exception )
    {
        this.exception = exception;
    }

    public void setMessage( String message, int priority )
    {
        this.message = message;
        this.priority = priority;
    }

    /**
     * Returns the exception that was thrown, if any. This field will only be
     * set for "taskFinished", "targetFinished", and "buildFinished" events.
     *
     * @return The Exception value
     * @see BuildListener#taskFinished(BuildEvent)
     * @see BuildListener#targetFinished(BuildEvent)
     * @see BuildListener#buildFinished(BuildEvent)
     */
    public Throwable getException()
    {
        return exception;
    }

    /**
     * Returns the logging message. This field will only be set for
     * "messageLogged" events.
     *
     * @return The Message value
     * @see BuildListener#messageLogged(BuildEvent)
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Returns the priority of the logging message. This field will only be set
     * for "messageLogged" events.
     *
     * @return The Priority value
     * @see BuildListener#messageLogged(BuildEvent)
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * Returns the project that fired this event.
     *
     * @return The Project value
     */
    public Project getProject()
    {
        return project;
    }

    /**
     * Returns the target that fired this event.
     *
     * @return The Target value
     */
    public Target getTarget()
    {

        return target;
    }

    /**
     * Returns the task that fired this event.
     *
     * @return The Task value
     */
    public Task getTask()
    {
        return task;
    }
}
