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
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.configurer.TaskContextAdapter;

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

    public void execute( final Configuration taskModel, final ExecutionFrame frame )
        throws TaskException
    {
        debug( "creating.notice" );
        final Task task = createTask( taskModel.getName(), frame );

        debug( "logger.notice" );
        doLogEnabled( task, taskModel, frame.getLogger() );

        debug( "contextualizing.notice" );
        doContextualize( task, taskModel, frame.getContext() );

        debug( "configuring.notice" );
        doConfigure( task, taskModel, frame.getContext() );

        debug( "executing.notice" );
        task.execute();
    }

    protected final void debug( final String key )
    {
        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( key );
            getLogger().debug( message );
        }
    }

    protected final Task createTask( final String name, final ExecutionFrame frame )
        throws TaskException
    {
        try
        {
            final TypeFactory factory = frame.getTypeManager().getFactory( Task.class );
            return (Task)factory.create( name );
        }
        catch( final TypeException te )
        {
            final String message = REZ.getString( "no-create.error", name );
            throw new TaskException( message, te );
        }
    }

    protected final void doConfigure( final Task task,
                                      final Configuration taskModel,
                                      final TaskContext taskContext )
        throws TaskException
    {
        try
        {
            final TaskContextAdapter context = new TaskContextAdapter( taskContext );
            m_configurer.configure( task, taskModel, context );
        }
        catch( final Throwable throwable )
        {
            final String message =
                REZ.getString( "config.error",
                               taskModel.getName(),
                               taskModel.getLocation(),
                               throwable.getMessage() );
            throw new TaskException( message, throwable );
        }
    }

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
                REZ.getString( "contextualize.error",
                               taskModel.getName(),
                               taskModel.getLocation(),
                               throwable.getMessage() );
            throw new TaskException( message, throwable );
        }
    }

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
                    REZ.getString( "logger.error",
                                   taskModel.getName(),
                                   taskModel.getLocation(),
                                   throwable.getMessage() );
                throw new TaskException( message, throwable );
            }
        }
    }
}
