/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api;

import java.io.File;

/**
 * This is the class that Task writers should extend to provide custom tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractTask
    implements Task
{
    ///Variable to hold context for use by sub-classes
    private TaskContext m_context;

    /**
     * Retrieve context from container.
     *
     * @param context the context
     */
    public void contextualize( final TaskContext context )
        throws TaskException
    {
        m_context = context;
    }

    /**
     * Execute task.
     * This method is called to perform actual work associated with task.
     * It is called after Task has been configured.
     *
     * @exception TaskException if an error occurs
     */
    public abstract void execute()
        throws TaskException;

    /**
     * Convenience method for sub-class to retrieve context.
     *
     * @return the context
     */
    protected final TaskContext getContext()
    {
        return m_context;
    }

    /**
     * Convenience method that returns the project's base directory.
     */
    protected final File getBaseDirectory()
    {
        return getContext().getBaseDirectory();
    }

    /**
     * Convenience method that locates a service for this task to use.
     *
     * @param serviceClass the service to locate.
     * @return the service, never returns null.
     * @throws TaskException if the service cannot be located.
     */
    protected final Object getService( final Class serviceClass )
        throws TaskException
    {
        return getContext().getService( serviceClass );
    }
}
