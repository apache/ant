/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import java.util.HashMap;
import java.util.Map;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;

/**
 * This contains details for each project that is being executed by a
 * DefaultWorkspace.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
final class ProjectEntry
{
    private final Project m_project;
    private final ExecutionFrame m_frame;

    /** Map from Target -> TargetState for that target. */
    private final Map m_targetState = new HashMap();

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

    public TargetState getTargetState( final Target target )
    {
        TargetState state = (TargetState)m_targetState.get( target );
        if( state == null )
        {
            state = TargetState.NOT_STARTED;
        }
        return state;
    }

    public void setTargetState( final Target target, final TargetState state )
    {
        m_targetState.put( target, state );
    }
}
