/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.tasklet.DefaultTaskletContext;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.ant.tasklet.engine.DefaultTaskletEngine;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.ant.util.Condition;
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

/**
 * This is the default implementation of ProjectEngine.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectEngine
    extends AbstractLoggable
    implements ProjectEngine, Composable
{
    protected TaskletEngine            m_taskletEngine;
    protected ProjectListenerSupport   m_listenerSupport = new ProjectListenerSupport();
    protected DefaultComponentManager  m_componentManager;

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
        m_taskletEngine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );
    }

    /**
     * Execute a target in a particular project.
     * Execute in the project context.
     *
     * @param project the Project
     * @param target the name of the target
     * @exception AntException if an error occurs
     */
    public void execute( final Project project, final String target )
        throws AntException
    {
        //HACK: should do this a better way !!!!!!
        m_componentManager.put( "org.apache.ant.project.Project", project );

        final TaskletContext context = project.getContext();

        final String projectName = (String)context.getProperty( Project.PROJECT );

        m_listenerSupport.projectStarted( projectName );

        //context = new DefaultTaskletContext( context );

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
     * @exception AntException if an error occurs
     */
    public void execute( Project project, String target, TaskletContext context )
        throws AntException
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
     * @exception AntException if an error occurs
     */
    protected void execute( final Project project,
                            final String targetName,
                            final TaskletContext context,
                            final ArrayList done )
        throws AntException
    {
        final Target target = project.getTarget( targetName );

        if( null == target )
        {
            throw new AntException( "Unable to find target " + targetName );
        }

        //add target to list of targets executed
        done.add( targetName );

        //execute all dependencies
        final Iterator dependencies = target.getDependencies();
        while( dependencies.hasNext() )
        {
            final String dependency = (String)dependencies.next();
            if( !done.contains( dependency ) )
            {
                execute( project, dependency, context, done );
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
     * @exception AntException if an error occurs
     */
    protected void executeTarget( final String targetName,
                                  final Target target,
                                  final TaskletContext context )
        throws AntException
    {
        //is this necessary ? I think not but ....
        // NO it isn't because you set target name and project has already been provided
        //m_componentManager.put( "org.apache.ant.project.Target", target );

        //create project context and set target name
        final TaskletContext targetContext = new DefaultTaskletContext( context );
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
    protected void executeTargetWork( final String name,
                                      final Target target,
                                      final TaskletContext context )
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
        final Iterator tasks = target.getTasks();
        while( tasks.hasNext() )
        {
            final Configuration task = (Configuration)tasks.next();
            executeTask( task, context );
        }
    }

    /**
     * Execute a task.
     *
     * @param task the task definition
     * @param context the context
     * @exception AntException if an error occurs
     */
    protected void executeTask( final Configuration task, final TaskletContext context )
        throws AntException
    {
        final String name = task.getName();
        getLogger().debug( "Executing task " + name );

        //Set up context for task...

        //is Only necessary if we are multi-threaded
        //final TaskletContext targetContext = new DefaultTaskletContext( context );

        //is setting name even necessary ???
        context.setProperty( TaskletContext.NAME, name );

        //notify listeners
        m_listenerSupport.taskletStarted( name );

        //run task
        m_taskletEngine.execute( task, context );

        //notify listeners task has ended
        m_listenerSupport.taskletFinished();
    }
}
