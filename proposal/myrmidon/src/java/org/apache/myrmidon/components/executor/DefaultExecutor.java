/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.executor;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.workspace.DefaultTaskContext;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * The basic executor that just executes the tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultExecutor
    extends AbstractLogEnabled
    implements Executor
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultExecutor.class );

    /**
     * Executes a task.
     */
    public void execute( final Configuration taskModel, final ExecutionFrame frame )
        throws TaskException
    {
        final String taskName = taskModel.getName();
        try
        {
            debug( "creating.notice", taskName );
            final Task task = doCreateTask( taskName, frame );

            debug( "contextualizing.notice", taskName );
            final TaskContext context = doCreateContext( frame );
            doContextualize( task, taskModel, context, frame );

            debug( "configuring.notice", taskName );
            doConfigure( task, taskModel, context, frame );

            debug( "executing.notice", taskName );
            task.execute();
        }
        catch( Exception e )
        {
            // Wrap in generic error message
            final String message = REZ.getString( "execute.error",
                                                  taskName, taskModel.getLocation() );
            throw new TaskException( message, e );
        }
    }

    protected final void debug( final String key, final String taskName )
    {
        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( key, taskName );
            getLogger().debug( message );
        }
    }

    /**
     * Creates a context for the task.
     */
    protected TaskContext doCreateContext( final ExecutionFrame frame )
    {
        // TODO - need to deactivate the context once the task has finished
        // executing
        return new DefaultTaskContext( frame.getServiceManager(),
                                       frame.getLogger(),
                                       frame.getProperties() );
    }

    /**
     * Creates a task instance.
     */
    protected final Task doCreateTask( final String name, final ExecutionFrame frame )
        throws TaskException
    {
        try
        {
            final TypeManager typeManager = (TypeManager)frame.getServiceManager().lookup( TypeManager.ROLE );
            final TypeFactory factory = typeManager.getFactory( Task.ROLE );
            return (Task)factory.create( name );
        }
        catch( final Exception te )
        {
            final String message = REZ.getString( "create.error", name );
            throw new TaskException( message, te );
        }
    }

    /**
     * Configures a task instance.
     */
    protected final void doConfigure( final Task task,
                                      final Configuration taskModel,
                                      final TaskContext taskContext,
                                      final ExecutionFrame frame )
        throws Exception
    {
        final Configurer configurer = (Configurer)frame.getServiceManager().lookup( Configurer.ROLE );
        configurer.configureElement( task, taskModel, taskContext );
    }

    /**
     * Sets the context for a task.
     */
    protected final void doContextualize( final Task task,
                                          final Configuration taskModel,
                                          final TaskContext taskContext,
                                          final ExecutionFrame frame )
        throws TaskException
    {
        try
        {
            task.contextualize( taskContext );
        }
        catch( final Throwable throwable )
        {
            final String message =
                REZ.getString( "contextualize.error", taskModel.getName() );
            throw new TaskException( message, throwable );
        }
    }
}
