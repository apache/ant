/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.aspects;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskException;

/**
 * AspectHandler is the interface through which aspects are handled.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class AbstractAspectHandler
    implements AspectHandler
{
    private Parameters m_aspectParameters;
    private Configuration[] m_aspectElements;

    private Task m_task;
    private Logger m_logger;
    private Configuration m_taskModel;

    public Configuration preCreate( final Configuration taskModel )
        throws TaskException
    {
        return taskModel;
    }

    public void aspectSettings( final Parameters parameters, final Configuration[] elements )
        throws TaskException
    {
        m_aspectParameters = parameters;
        m_aspectElements = elements;
    }

    public void postCreate( final Task task )
        throws TaskException
    {
        m_task = task;
    }

    public void preLogEnabled( final Logger logger )
        throws TaskException
    {
        m_logger = logger;
    }

    public void preConfigure( final Configuration taskModel )
        throws TaskException
    {
        m_taskModel = taskModel;
    }

    public void preExecute()
        throws TaskException
    {
    }

    public void preDestroy()
        throws TaskException
    {
        reset();
    }

    public boolean error( final TaskException te )
        throws TaskException
    {
        reset();
        return false;
    }

    protected void reset()
    {
        m_aspectParameters = null;
        m_aspectElements = null;
        m_task = null;
        m_logger = null;
        m_taskModel = null;
    }

    protected final Configuration getTaskModel()
    {
        return m_taskModel;
    }

    protected final Task getTask()
    {
        return m_task;
    }

    protected final Logger getLogger()
    {
        return m_logger;
    }

    protected final Configuration[] getAspectElements()
    {
        return m_aspectElements;
    }

    protected final Parameters getAspectParameters()
    {
        return m_aspectParameters;
    }
}
