/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import junit.framework.Assert;
import org.apache.myrmidon.listeners.LogEvent;
import org.apache.myrmidon.listeners.ProjectEvent;
import org.apache.myrmidon.listeners.ProjectListener;
import org.apache.myrmidon.listeners.TargetEvent;
import org.apache.myrmidon.listeners.TaskEvent;

/**
 * A project listener that asserts that it receives a particular sequence of
 * events.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class TrackingProjectListener
    extends Assert
    implements ProjectListener
{
    private String m_rootProject;
    private String m_currentProject;
    private String m_currentTarget;
    private String m_currentTask;

    /**
     * Notify the listener that a project is about to start.
     */
    public void projectStarted( final ProjectEvent event )
    {
        assertNull( "Project already started", m_rootProject );
        m_rootProject = event.getProjectName();
    }

    /**
     * Notify the listener that a project has finished.
     */
    public void projectFinished( final ProjectEvent event )
    {
        assertEquals( "Mismatched project name", m_rootProject, event.getProjectName() );
        m_rootProject = null;

        assertNull( "Target not started", m_currentTarget );
    }

    /**
     * Notify the listener that a target is about to start.
     */
    public void targetStarted( final TargetEvent event )
    {
        assertNotNull( "Project not started", m_rootProject );
        assertNull( "Target already started", m_currentTarget );
        m_currentProject = event.getProjectName();
        m_currentTarget = event.getTargetName();
    }

    /**
     * Notify the listener that a target has finished.
     */
    public void targetFinished( final TargetEvent event )
    {
        assertEquals( "Mismatched project name", m_currentProject, event.getProjectName() );
        assertEquals( "Mismatched target name", m_currentTarget, event.getTargetName() );
        m_currentProject = null;
        m_currentTarget = null;

        assertNull( "Task not finished", m_currentTask );
    }

    /**
     * Notify the listener that a task is about to start.
     */
    public void taskStarted( final TaskEvent event )
    {
        assertEquals( "Mismatched project name", m_currentProject, event.getProjectName() );
        assertEquals( "Mismatched target name", m_currentTarget, event.getTargetName() );

        assertNull( "Task already started", m_currentTask );
        m_currentTask = event.getTaskName();
    }

    /**
     * Notify the listener that a task has finished.
     */
    public void taskFinished( final TaskEvent event )
    {
        assertEquals( "Mismatched project name", m_currentProject, event.getProjectName() );
        assertEquals( "Mismatched target name", m_currentTarget, event.getTargetName() );
        assertEquals( "Mismatched task name", m_currentTask, event.getTaskName() );
        m_currentTask = null;
    }

    /**
     * Notify listener of log message event.
     */
    public void log( final LogEvent event )
    {
        assertEquals( "Mismatched project name", m_currentProject, event.getProjectName() );
        assertEquals( "Mismatched target name", m_currentTarget, event.getTargetName() );
        assertEquals( "Mismatched task name", m_currentTask, event.getTaskName() );
        assertNull( "Unexpected build error", event.getThrowable() );
    }

    /**
     * Asserts that the listener has finished.
     */
    public void assertComplete()
    {
        assertNull( "Task not finished", m_currentTask );
        assertNull( "Target not finished", m_currentTarget );
        assertNull( "Target not finished", m_currentProject );
        assertNull( "Project not finished", m_rootProject );
    }
}
