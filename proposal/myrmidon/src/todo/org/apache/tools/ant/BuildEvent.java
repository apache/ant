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
    private int m_priority = Project.MSG_VERBOSE;
    private Throwable m_exception;
    private String m_message;
    private String m_target;
    private String m_task;

    /**
     * Construct a BuildEvent for a target level event
     *
     * @param target the target that emitted the event.
     */
    public BuildEvent( String target )
    {
        super( target );
        m_target = target;
    }

    public void setException( Throwable exception )
    {
        m_exception = exception;
    }

    public void setMessage( String message, int priority )
    {
        m_message = message;
        m_priority = priority;
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
        return m_exception;
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
        return m_message;
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
        return m_priority;
    }

    /**
     * Returns the target that fired this event.
     *
     * @return The Target value
     */
    public String getTarget()
    {
        return m_target;
    }

    /**
     * Returns the task that fired this event.
     *
     * @return The Task value
     */
    public String getTask()
    {
        return m_task;
    }
}
