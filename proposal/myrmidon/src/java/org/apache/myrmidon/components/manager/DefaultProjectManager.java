/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.manager;

import java.util.ArrayList;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.log.Logger;
import org.apache.myrmidon.api.DefaultTaskContext;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.model.Condition;
import org.apache.myrmidon.components.model.Project;
import org.apache.myrmidon.components.model.Target;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * This is the default implementation of ProjectEngine.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectManager
    extends AbstractLoggable
    implements ProjectManager, Composable
{
    private Executor                 m_executor;
    private ProjectListenerSupport   m_listenerSupport = new ProjectListenerSupport();
    private DefaultComponentManager  m_componentManager;

    /**
     * Add a listener to project events.
     *
     * @param listener the listener
     */
    public void addProjectListener( final ProjectListener listener )
    {
        m_listenerSupport.addProjectListener( listener );
    }

    /**
     * Remove a listener from project events.
     *
     * @param listener the listener
     */
    public void removeProjectListener( final ProjectListener listener )
    {
        m_listenerSupport.removeProjectListener( listener );
    }

    /**
     * Retrieve relevent services needed for engine.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_componentManager = (DefaultComponentManager)componentManager;
        m_executor = (Executor)componentManager.lookup( Executor.ROLE );
    }

    /**
     * Execute a target in a particular project.
     * Execute in the project context.
     *
     * @param project the Project
     * @param target the name of the target
     * @exception TaskException if an error occurs
     */
    public void executeTarget( final Project project, final String target, final TaskContext context )
        throws TaskException
    {
        //HACK: should do this a better way !!!!!!
        m_componentManager.put( Project.ROLE, project );

        m_listenerSupport.projectStarted();

        executeTargetWork( "<init>", project.getImplicitTarget(), context );

        execute( project, target, context );

        m_listenerSupport.projectFinished();
    }

    /**
     * Execute a target in a particular project, in a particular context.
     *
     * @param project the Project
     * @param target the name of the target
     * @param context the context
     * @exception TaskException if an error occurs
     */
    public void execute( Project project, String target, TaskContext context )
        throws TaskException
    {
        execute( project, target, context, new ArrayList() );
    }

    /**
     * Helper method to execute a target.
     *
     * @param project the Project
     * @param target the name of the target
     * @param context the context
     * @param done the list of targets already executed in current run
     * @exception TaskException if an error occurs
     */
    private void execute( final Project project,
                          final String targetName,
                          final TaskContext context,
                          final ArrayList done )
        throws TaskException
    {
        final Target target = project.getTarget( targetName );

        if( null == target )
        {
            throw new TaskException( "Unable to find target " + targetName );
        }

        //add target to list of targets executed
        done.add( targetName );

        //execute all dependencies
        final String[] dependencies = target.getDependencies();
        for( int i = 0; i < dependencies.length; i++ )
        {
            if( !done.contains( dependencies[ i ] ) )
            {
                execute( project, dependencies[ i ], context, done );
            }
        }

        executeTarget( targetName, target, context );
    }

    /**
     * Method to execute a particular target instance.
     *
     * @param targetName the name of target
     * @param target the target
     * @param context the context in which to execute
     * @exception TaskException if an error occurs
     */
    private void executeTarget( final String targetName,
                                final Target target,
                                final TaskContext context )
        throws TaskException
    {
        //is this necessary ? I think not but ....
        // NO it isn't because you set target name and project has already been provided
        //m_componentManager.put( "org.apache.ant.project.Target", target );

        //create project context and set target name
        final TaskContext targetContext = new DefaultTaskContext( context );
        targetContext.setProperty( Project.TARGET, targetName );

        //notify listeners
        m_listenerSupport.targetStarted( targetName );

        //actually do the execution work
        executeTargetWork( targetName, target, targetContext );

        //notify listeners
        m_listenerSupport.targetFinished();
    }

    /**
     * Do the work associated with target.
     * ie execute all tasks
     *
     * @param name the name of target
     * @param target the target
     * @param context the context
     */
    private void executeTargetWork( final String name,
                                    final Target target,
                                    final TaskContext context )
        throws TaskException
    {
        //check the condition associated with target.
        //if it is not satisfied then skip target
        final Condition condition = target.getCondition();
        if( null != condition )
        {
            if( false == condition.evaluate( context ) )
            {
                getLogger().debug( "Skipping target " + name +
                                   " as it does not satisfy condition" );
                return;
            }
        }

        getLogger().debug( "Executing target " + name );

        //execute all tasks assciated with target
        final Configuration[] tasks = target.getTasks();
        for( int i = 0; i < tasks.length; i++ )
        {
            executeTask( tasks[ i ], context );
        }
    }

    /**
     * Execute a task.
     *
     * @param task the task definition
     * @param context the context
     * @exception TaskException if an error occurs
     */
    private void executeTask( final Configuration task, final TaskContext context )
        throws TaskException
    {
        final String name = task.getName();
        getLogger().debug( "Executing task " + name );

        //Set up context for task...
        //is Only necessary if we are multi-threaded
        //final TaskletContext targetContext = new DefaultTaskletContext( context );

        //is setting name even necessary ???
        context.setProperty( TaskContext.NAME, name );

        //notify listeners
        m_listenerSupport.taskStarted( name );

        //run task
        m_executor.execute( task, context );

        //notify listeners task has ended
        m_listenerSupport.taskFinished();
    }
}
