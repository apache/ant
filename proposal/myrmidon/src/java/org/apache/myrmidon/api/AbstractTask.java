/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api;

import java.io.File;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * This is the class that Task writers should extend to provide custom tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public abstract class AbstractTask
    extends AbstractLogEnabled
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
     * It is called after Task has been Configured and Initialized and before
     * beig Disposed (If task implements appropriate interfaces).
     *
     * @exception Exception if an error occurs
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

    protected final Object get( final Object key )
        throws ContextException
    {
        return getContext().get( key );
    }

    //Needs to be made protected
    public final File getBaseDirectory()
    {
        return getContext().getBaseDirectory();
    }

    protected final File resolveFile( final String filename )
        throws TaskException
    {
        return getContext().resolveFile( filename );
    }

    protected final Object resolveValue( final String value )
        throws TaskException
    {
        return getContext().resolveValue( value );
    }

    protected final Object getProperty( final String name )
    {
        return getContext().getProperty( name );
    }

    protected final Object getService( final Class serviceClass )
        throws TaskException
    {
        return getContext().getService( serviceClass );
    }

    protected final TaskContext createSubContext( final String name )
        throws TaskException
    {
        return getContext().createSubContext( name );
    }
}
