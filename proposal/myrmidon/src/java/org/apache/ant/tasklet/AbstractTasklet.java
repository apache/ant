/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet;

import org.apache.ant.AntException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLoggable;

/**
 * This is abstract base class for tasklets.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public abstract class AbstractTasklet
    extends AbstractLoggable
    implements Tasklet, Contextualizable, Initializable, Disposable
{
    ///Variable to hold context for use by sub-classes
    private TaskletContext            m_context;

    /**
     * Retrieve context from container.
     *
     * @param context the context
     */
    public void contextualize( final Context context )
    {
        m_context = (TaskletContext)context;
    }

    /**
     * This will be called before execute() method and checks any preconditions.
     *
     * @exception Exception if an error occurs
     */
    public void initialize()
        throws Exception
    {
    }

    /**
     * This will be called after execute() method.
     * Use this to clean up any resources associated with task.
     *
     * @exception Exception if an error occurs
     */
    public void dispose()
        throws Exception
    {
    }

    /**
     * Convenience method for sub-class to retrieve context.
     *
     * @return the context
     */
    protected final TaskletContext getContext()
    {
        return m_context;
    }
}
