/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

/**
 * Abstract listener from which to extend.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractProjectListener
    implements ProjectListener
{
    /**
     * This contains the name of the current target.
     */
    private String m_target;

    /**
     * This contains the name of the current task.
     */
    private String m_task;

    /**
     * Notify listener of projectStarted event.
     */
    public void projectStarted()
    {
    }

    /**
     * Notify listener of projectFinished event.
     */
    public void projectFinished()
    {
    }

    /**
     * Notify listener of targetStarted event.
     *
     * @param targetName the name of target
     */
    public void targetStarted( final String target )
    {
        m_target = target;
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished()
    {
        m_target = null;
    }

    /**
     * Notify listener of taskStarted event.
     *
     * @param task the name of task
     */
    public void taskStarted( final String task )
    {
        m_task = task;
    }

    /**
     * Notify listener of taskFinished event.
     */
    public void taskFinished()
    {
        m_task = null;
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     */
    public void log( String message )
    {
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void log( String message, Throwable throwable )
    {
    }

    /**
     * Utility method to get the name of current target.
     */
    protected final String getTarget()
    {
        return m_target;
    }

    /**
     * Utility method to get the name of current task.
     */
    protected final String getTask()
    {
        return m_task;
    }
}
