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
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;

/**
 * The basic executor that just executes the tasks.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultExecutor
    extends AbstractLogEnabled
    implements Executor, Serviceable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultExecutor.class );

    private Configurer m_configurer;

    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param serviceManager the ServiceManager
     * @exception ServiceException if an error occurs
     */
    public void service( final ServiceManager serviceManager )
        throws ServiceException
    {
        m_configurer = (Configurer)serviceManager.lookup( Configurer.ROLE );
    }

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

            debug( "logger.notice", taskName );
            doLogEnabled( task, taskModel, frame.getLogger() );

            debug( "contextualizing.notice", taskName );
            doContextualize( task, taskModel, frame.getContext() );

            debug( "configuring.notice", taskName );
            doConfigure( task, taskModel, frame.getContext() );

            debug( "executing.notice", taskName );
            task.execute();
        }
        catch( Exception e )
        {
            // Wrap in generic error message
            final String message = REZ.getString( "execute.error", taskName, taskModel.getLocation() );
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
     * Creates a task instance.
     */
    protected final Task doCreateTask( final String name, final ExecutionFrame frame )
        throws TaskException
    {
        try
        {
            final TypeFactory factory = frame.getTypeManager().getFactory( Task.class );
            return (Task)factory.create( name );
        }
        catch( final TypeException te )
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
                                      final TaskContext taskContext )
        throws ConfigurationException
    {
        m_configurer.configure( task, taskModel, taskContext );
    }

    /**
     * Sets the context for a task.
     */
    protected final void doContextualize( final Task task,
                                          final Configuration taskModel,
                                          final TaskContext context )
        throws TaskException
    {
        try
        {
            task.contextualize( context );
        }
        catch( final Throwable throwable )
        {
            final String message =
                REZ.getString( "contextualize.error", taskModel.getName() );
            throw new TaskException( message, throwable );
        }
    }

    /**
     * Sets the logger for a task.
     */
    protected final void doLogEnabled( final Task task,
                                       final Configuration taskModel,
                                       final Logger logger )
        throws TaskException
    {
        if( task instanceof LogEnabled )
        {
            try
            {
                ( (LogEnabled)task ).enableLogging( logger );
            }
            catch( final Throwable throwable )
            {
                final String message =
                    REZ.getString( "logger.error", taskModel.getName() );
                throw new TaskException( message, throwable );
            }
        }
    }
}
