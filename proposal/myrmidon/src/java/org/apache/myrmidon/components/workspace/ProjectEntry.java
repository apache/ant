/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import java.util.ArrayList;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.model.Project;

/**
 * This contains detaisl for each project that is managed by ProjectManager.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public final class ProjectEntry
{
    private final Project m_project;
    private final ExecutionFrame m_frame;
    private final ArrayList m_targetsCompleted = new ArrayList();

    public ProjectEntry( final Project project,
                         final ExecutionFrame frame )
    {
        m_project = project;
        m_frame = frame;
    }

    public Project getProject()
    {
        return m_project;
    }

    public ExecutionFrame getFrame()
    {
        return m_frame;
    }

    public boolean isTargetCompleted( final String target )
    {
        return m_targetsCompleted.contains( target );
    }

    public void completeTarget( final String target )
    {
        m_targetsCompleted.add( target );
    }
}
