/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.listeners;

import org.apache.avalon.framework.ExceptionUtil;

/**
 * Abstract listener from which to extend.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class AbstractProjectListener
    implements ProjectListener
{
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
    public void targetStarted( final String targetName )
    {
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished()
    {
    }

    /**
     * Notify listener of taskStarted event.
     *
     * @param taskName the name of task
     */
    public void taskStarted( final String taskName )
    {
    }

    /**
     * Notify listener of taskFinished event.
     */
    public void taskFinished()
    {
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
}
