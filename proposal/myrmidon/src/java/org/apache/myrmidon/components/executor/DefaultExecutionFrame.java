/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.executor;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.log.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.aspect.AspectManager;
import org.apache.myrmidon.components.builder.ProjectBuilder;
import org.apache.myrmidon.components.configurer.Configurer;
import org.apache.myrmidon.components.converter.ConverterRegistry;
import org.apache.myrmidon.components.converter.MasterConverter;
import org.apache.myrmidon.components.deployer.Deployer;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.manager.ProjectManager;
import org.apache.myrmidon.components.role.RoleManager;
import org.apache.myrmidon.components.type.TypeManager;

/**
 * Frames in which tasks are executed.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultExecutionFrame
    implements ExecutionFrame, Loggable, Contextualizable, Composable
{
    private TypeManager              m_typeManager;

    private Logger                   m_logger;
    private TaskContext              m_context;
    private ComponentManager         m_componentManager;

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    public void contextualize( final Context context )
    {
        m_context = (TaskContext)context;
    }

    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_componentManager = componentManager;

        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
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

    public ComponentManager getComponentManager()
    {
        return m_componentManager;
    }
}
