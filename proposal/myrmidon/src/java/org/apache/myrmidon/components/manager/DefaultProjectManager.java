/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Logger;
import org.apache.myrmidon.api.DefaultTaskContext;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.executor.DefaultExecutionFrame;
import org.apache.myrmidon.components.executor.ExecutionFrame;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.framework.Condition;
import org.apache.myrmidon.components.model.Project;
import org.apache.myrmidon.components.model.Target;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * This is the default implementation of ProjectEngine.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectManager
    extends AbstractLoggable
    implements ProjectManager, Composable, Parameterizable, Initializable
{
    private Executor                 m_executor;
    private ProjectListenerSupport   m_listenerSupport   = new ProjectListenerSupport();
    private ComponentManager         m_componentManager;
    private Parameters               m_parameters;
    private Project                  m_project;
    private TaskContext              m_baseContext;
    private HashMap                  m_entrys            = new HashMap();
    private TypeManager              m_typeManager;

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
        m_componentManager = componentManager;

        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_executor = (Executor)componentManager.lookup( Executor.ROLE );
        m_project = (Project)componentManager.lookup( Project.ROLE );
    }

    public void parameterize( final Parameters parameters )
        throws ParameterException
    {
        m_parameters = parameters;
    }

    public void initialize()
        throws Exception
    {
        m_baseContext = createBaseContext();
    }

    /**
     * Execute a target in a particular project.
     * Execute in the project context.
     *
     * @param project the Project
     * @param target the name of the target
     * @exception TaskException if an error occurs
     */
    public void executeProject( final Project project, final String target )
        throws TaskException
    {
        final ProjectEntry entry = getProjectEntry( project );

        m_listenerSupport.projectStarted();

        executeTarget( "<init>", project.getImplicitTarget(), entry.getFrame() );

        execute( project, target, entry );

        m_listenerSupport.projectFinished();
    }


    private TaskContext createBaseContext()
        throws TaskException
    {
        final TaskContext context = new DefaultTaskContext();

        final String[] names = m_parameters.getNames();
        for( int i = 0; i < names.length; i++ )
        {
            final String value = m_parameters.getParameter( names[ i ], null );
            context.setProperty( names[ i ], value );
        }

        //Add system properties so that they overide user-defined properties
        addToContext( context, System.getProperties() );

        return context;
    }

    private ExecutionFrame createExecutionFrame( final Project project )
        throws TaskException
    {
        final TaskContext context = new DefaultTaskContext( m_baseContext );
        context.setProperty( TaskContext.BASE_DIRECTORY, project.getBaseDirectory() );

        //Create per frame ComponentManager
        final DefaultComponentManager componentManager = 
            new DefaultComponentManager( m_componentManager );

        //Add in child type manager so each frame can register different 
        //sets of tasks etc
        componentManager.put( TypeManager.ROLE, m_typeManager.createChildTypeManager() );

        //We need to place projects and ProjectManager
        //in ComponentManager so as to support project-local call()
        componentManager.put( ProjectManager.ROLE, this );
        componentManager.put( Project.ROLE, project );

        final String[] names = project.getProjectNames();
        for( int i = 0; i < names.length; i++ )
        {
            final String name = names[ i ];
            final Project other = project.getProject( name );
            componentManager.put( Project.ROLE + "/" + name, other );
        }

        final DefaultExecutionFrame frame = new DefaultExecutionFrame();
               
        try
        {

            frame.setLogger( getLogger() );
            frame.contextualize( context );
            frame.compose( componentManager );
        }
        catch( final Exception e )
        {
            throw new TaskException( "Error setting up ExecutionFrame", e );
        }

        return frame;
    }

    private ProjectEntry getProjectEntry( final Project project )
        throws TaskException
    {
        ProjectEntry entry = (ProjectEntry)m_entrys.get( project );
        
        if( null == entry )
        {
            final ExecutionFrame frame = createExecutionFrame( project );
            entry = new ProjectEntry( project, frame );
            m_entrys.put( project, entry );
        }

        return entry;
    }

    private Project getProject( final String name, final Project project )
        throws TaskException
    {
        final Project other = project.getProject( name );
        
        if( null == other )
        {
            //TODO: Fix this so location information included in description
            throw new TaskException( "Project '" + name + "' not found." );
        }

        return other;
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
                          final ProjectEntry entry )
        throws TaskException
    {
        final int index = targetName.indexOf( "->" );
        if( -1 != index )
        {
            final String name = targetName.substring( 0, index );
            final String otherTargetName = targetName.substring( index + 2 );

            final Project otherProject = getProject( name, project );
            final ProjectEntry otherEntry = getProjectEntry( otherProject );

            //Execute target in referenced project 
            execute( otherProject, otherTargetName, otherEntry );
            return;
        }

        final Target target = project.getTarget( targetName );
        if( null == target )
        {
            throw new TaskException( "Unable to find target " + targetName );
        }

        //add target to list of targets executed
        entry.completeTarget( targetName );

        //execute all dependencies
        final String[] dependencies = target.getDependencies();
        for( int i = 0; i < dependencies.length; i++ )
        {
            if( !entry.isTargetCompleted( dependencies[ i ] ) )
            {
                execute( project, dependencies[ i ], entry );
            }
        }

        //notify listeners
        m_listenerSupport.targetStarted( targetName );

        executeTarget( targetName, target, entry.getFrame() );

        //notify listeners
        m_listenerSupport.targetFinished();
    }

    /**
     * Method to execute a particular target instance.
     *
     * @param targetName the name of target
     * @param target the target
     * @param context the context in which to execute
     * @exception TaskException if an error occurs
     */
    private void executeTarget( final String name,
                                final Target target,
                                final ExecutionFrame frame )
        throws TaskException
    {
        //check the condition associated with target.
        //if it is not satisfied then skip target
        final Condition condition = target.getCondition();
        if( null != condition )
        {
            if( false == condition.evaluate( frame.getContext() ) )
            {
                getLogger().debug( "Skipping target " + name +
                                   " as it does not satisfy condition" );
                return;
            }
        }

        getLogger().debug( "Executing target " + name );

        //frame.getContext().setProperty( Project.TARGET, target );

        //execute all tasks assciated with target
        final Configuration[] tasks = target.getTasks();
        for( int i = 0; i < tasks.length; i++ )
        {
            executeTask( tasks[ i ], frame );
        }
    }

    /**
     * Execute a task.
     *
     * @param task the task definition
     * @param context the context
     * @exception TaskException if an error occurs
     */
    private void executeTask( final Configuration task, final ExecutionFrame frame )
        throws TaskException
    {
        final String name = task.getName();
        getLogger().debug( "Executing task " + name );

        //is setting name even necessary ???
        frame.getContext().setProperty( TaskContext.NAME, name );

        //notify listeners
        m_listenerSupport.taskStarted( name );

        //run task
        m_executor.execute( task, frame );

        //notify listeners task has ended
        m_listenerSupport.taskFinished();
    }

    /**
     * Helper method to add values to a context
     *
     * @param context the context
     * @param map the map of names->values
     */
    private void addToContext( final TaskContext context, final Map map )
        throws TaskException
    {
        final Iterator keys = map.keySet().iterator();

        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final Object value = map.get( key );
            context.setProperty( key, value );
        }
    }
}
