/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

/**
 * Abstract listener from which to extend.  This implementation provedes
 * empty implementions of each of the event methods.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractProjectListener
    implements ProjectListener
{
    /**
     * Notify listener of projectStarted event.
     */
    public void projectStarted( final ProjectEvent event )
    {
    }

    /**
     * Notify listener of projectFinished event.
     */
    public void projectFinished( final ProjectEvent event )
    {
    }

    /**
     * Notify listener of targetStarted event.
     */
    public void targetStarted( final TargetEvent event )
    {
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished( final TargetEvent event )
    {
    }

    /**
     * Notify listener of taskStarted event.
     */
    public void taskStarted( final TaskEvent event )
    {
    }

    /**
     * Notify listener of taskFinished event.
     */
    public void taskFinished( final TaskEvent event )
    {
    }

    /**
     * Notify listener of log message event.
     */
    public void log( final LogEvent event )
    {
    }
}
