/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Frames in which tasks are executed.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class DefaultExecutionFrame
    implements ExecutionFrame, Component
{
    private final Logger m_logger;
    private final TaskContext m_context;
    private final TypeManager m_typeManager;

    public DefaultExecutionFrame( final Logger logger,
                                  final TaskContext context,
                                  final TypeManager typeManager )
    {
        m_logger = logger;
        m_context = context;
        m_typeManager = typeManager;
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
