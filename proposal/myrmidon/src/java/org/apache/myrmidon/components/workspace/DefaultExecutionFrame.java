/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Frames in which tasks are executed.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class DefaultExecutionFrame
    implements ExecutionFrame, Component, LogEnabled, Contextualizable
{
    private Logger m_logger;
    private TaskContext m_context;
    private TypeManager m_typeManager;

    public void enableLogging( final Logger logger )
    {
        m_logger = logger;
    }

    public void contextualize( final Context context )
        throws ContextException
    {
        m_context = (TaskContext)context;
        try
        {
            m_typeManager = (TypeManager)m_context.getService( TypeManager.class );
        }
        catch( TaskException te )
        {
            throw new ContextException( te.getMessage(), te );
        }
    }

    public TypeManager getTypeManager()
    {
        return m_typeManager;
    }

    public Logger getLogger()
    {
        return m_logger;
    }

    public TaskContext getContext()
    {
        return m_context;
    }
}
