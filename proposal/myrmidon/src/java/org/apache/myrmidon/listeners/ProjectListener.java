/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.listeners;

/**
 * The interface to implement if you want to receive
 * notification of project status.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:role shorthand="listener"
 * @todo Think about having a way to indicate that a foreign project
 *       is being referenced, a implicit target is being referenced
 *       and that a library is being imported.
 */
public interface ProjectListener
{
    String ROLE = ProjectListener.class.getName();

    /**
     * Notify the listener that a project is about to start.  This method
     * is called for top-level projects only.
     */
    void projectStarted( ProjectEvent event );

    /**
     * Notify the listener that a project has finished.  This method is called
     * for top-level projects only.
     */
    void projectFinished( ProjectEvent event );

    /**
     * Notify the listener that a target is about to start.  Note that the
     * project name reported by the event may be different to that reported
     * in {@link #projectStarted}.
     */
    void targetStarted( TargetEvent event );

    /**
     * Notify the listener that a target has finished.
     */
    void targetFinished( TargetEvent event );

    /**
     * Notify the listener that a task is about to start.
     */
    void taskStarted( TaskEvent event );

    /**
     * Notify the listener that a task has finished.
     */
    void taskFinished( TaskEvent event );

    /**
     * Notify listener of log message event.  Note that this method may
     * be called at any time, so the reported task, target, or project names
     * may be null.
     */
    void log( LogEvent event );
}
