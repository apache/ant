/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import java.io.File;
import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.executor.ExecutionContainer;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.model.Dependency;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;
import org.apache.myrmidon.interfaces.model.TypeLib;
import org.apache.myrmidon.interfaces.property.PropertyStore;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * This is the default implementation of Workspace.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultWorkspace
    extends AbstractLogEnabled
    implements Workspace, ExecutionContainer, Parameterizable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultWorkspace.class );

    private Executor m_executor;
    private ProjectListenerSupport m_listenerSupport = new ProjectListenerSupport();
    private ServiceManager m_serviceManager;
    private Parameters m_parameters;
    private PropertyStore m_baseStore;
    private TypeManager m_typeManager;
    private Deployer m_deployer;

    /** A map from Project object -> ProjectEntry for that project. */
    private HashMap m_entries = new HashMap();

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
     * Sets the root execution frame.
     */
    public void setRootExecutionFrame( final ExecutionFrame frame ) throws Exception
    {
        m_baseStore = frame.getProperties();
        m_serviceManager = frame.getServiceManager();
        m_typeManager = (TypeManager)m_serviceManager.lookup( TypeManager.ROLE );
        m_executor = (Executor)m_serviceManager.lookup( Executor.ROLE );
        m_deployer = (Deployer)m_serviceManager.lookup( Deployer.ROLE );
    }

    public void parameterize( final Parameters parameters )
        throws ParameterException
    {
        m_parameters = parameters;
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

        m_listenerSupport.projectStarted( project.getProjectName() );

        executeTarget( entry, target );

        m_listenerSupport.projectFinished( project.getProjectName() );
    }

    private File findTypeLib( final String libraryName )
        throws Exception
    {
        //TODO: In future this will be expanded to allow
        //users to specify search path or automagically
        //add entries to lib path (like user specific or
        //workspace specific)
        final String name = libraryName.replace( '/', File.separatorChar ) + ".atl";

        final String home = m_parameters.getParameter( "myrmidon.home" );
        final File homeDir = new File( home + File.separatorChar + "ext" );

        final File library = new File( homeDir, name );

        if( library.exists() )
        {
            if( !library.canRead() )
            {
                final String message = REZ.getString( "no-read.error", library );
                throw new TaskException( message );
            }
            else
            {
                return library;
            }
        }

        final String message = REZ.getString( "no-library.error", libraryName );
        throw new TaskException( message );
    }

    private void deployTypeLib( final Deployer deployer, final Project project )
        throws Exception
    {
        final TypeLib[] typeLibs = project.getTypeLibs();

        for( int i = 0; i < typeLibs.length; i++ )
        {
            final TypeLib typeLib = typeLibs[ i ];
            final File file = findTypeLib( typeLib.getLibrary() );

            try
            {
                final TypeDeployer typeDeployer = deployer.createDeployer( file );
                if( null == typeLib.getRole() )
                {
                    // Deploy everything in the typelib
                    typeDeployer.deployAll();
                }
                else
                {
                    // Deploy the specified type
                    typeDeployer.deployType( typeLib.getRole(), typeLib.getName() );
                }
            }
            catch( final DeploymentException de )
            {
                final String message = REZ.getString( "no-deploy.error",
                                                      typeLib.getLibrary(), file );
                throw new TaskException( message, de );
            }
        }
    }

    /**
     * Creates an execution frame for a project.
     */
    private ExecutionFrame createExecutionFrame( final Project project )
        throws Exception
    {
        //Create per frame ComponentManager
        final DefaultServiceManager serviceManager =
            new DefaultServiceManager( m_serviceManager );

        //Add in child type manager so each frame can register different
        //sets of tasks etc
        final TypeManager typeManager = m_typeManager.createChildTypeManager();
        serviceManager.put( TypeManager.ROLE, typeManager );

        // TODO - Add child role manager and configurer

        //We need to create a new deployer so that it deploys
        //to project specific TypeManager
        final Deployer deployer = m_deployer.createChildDeployer( serviceManager );
        serviceManager.put( Deployer.ROLE, deployer );

        // Deploy the imported typelibs
        deployTypeLib( deployer, project );

        //We need to place projects and ProjectManager
        //in ComponentManager so as to support project-local call()
        // TODO - add project to properties, not services
        serviceManager.put( Workspace.ROLE, this );
        serviceManager.put( Project.ROLE, project );

        // Create a logger
        final Logger logger =
            new RoutingLogger( getLogger(), m_listenerSupport );

        // Properties
        final PropertyStore store = m_baseStore.createChildStore("");
        store.setProperty( TaskContext.BASE_DIRECTORY, project.getBaseDirectory() );

        final DefaultExecutionFrame frame =
            new DefaultExecutionFrame( logger, store, serviceManager );

        /**
         *  @todo Should no occur but done for the time being to simplify evolution.
         */
        serviceManager.put( ExecutionFrame.ROLE, frame );

        return frame;
    }

    private ProjectEntry getProjectEntry( final Project project )
        throws TaskException
    {
        ProjectEntry entry = (ProjectEntry)m_entries.get( project );

        if( null == entry )
        {
            try
            {
                final ExecutionFrame frame = createExecutionFrame( project );
                entry = new ProjectEntry( project, frame );
                m_entries.put( project, entry );
            }
            catch( Exception e )
            {
                final String message = REZ.getString( "bad-frame.error" );
                throw new TaskException( message, e );
            }
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
            final String message = REZ.getString( "no-project.error", name );
            throw new TaskException( message );
        }

        return other;
    }

    /**
     * Helper method to execute a target.
     *
     * @param entry the project to execute
     * @param targetName the name of the target to execute
     * @exception TaskException if an error occurs
     */
    private void executeTarget( final ProjectEntry entry,
                                final String targetName )
        throws TaskException
    {
        // Locate the target
        final Target target = entry.getProject().getTarget( targetName );
        if( null == target )
        {
            final String message = REZ.getString( "no-target.error", targetName );
            throw new TaskException( message );
        }

        executeTarget( entry, targetName, target );
    }

    /**
     * Executes a target.  Does not execute the target if it has already been
     * executed.  Executes the dependencies of the target, before executing
     * the target itself.
     *
     * @param name the name of target
     * @param target the target
     * @param entry the project in which to execute
     * @exception TaskException if an error occurs
     */
    private void executeTarget( final ProjectEntry entry,
                                final String name,
                                final Target target )
        throws TaskException
    {
        final Project project = entry.getProject();

        // Check target state, to see if it has already been executed, and
        // to check for dependency cycles
        final TargetState state = entry.getTargetState( target );
        if( state == TargetState.FINISHED )
        {
            // Target has been executed
            return;
        }
        if( state == TargetState.TRAVERSING )
        {
            // Cycle in target dependencies
            final String message = REZ.getString( "target-dependency-cycle.error", name );
            throw new TaskException( message );
        }

        // Set state to indicate this target has been started
        entry.setTargetState( target, TargetState.TRAVERSING );

        // Execute the target's dependencies

        // Implicit target first
        if( target != project.getImplicitTarget() )
        {
            executeTarget( entry, "<init>", project.getImplicitTarget() );
        }

        // Named dependencies
        final Dependency[] dependencies = target.getDependencies();
        for( int i = 0; i < dependencies.length; i++ )
        {
            final Dependency dependency = dependencies[ i ];
            final String otherProjectName = dependency.getProjectName();
            if( otherProjectName != null )
            {
                // Dependency in a referenced project
                final Project otherProject = getProject( otherProjectName, project );
                final ProjectEntry otherEntry = getProjectEntry( otherProject );
                executeTarget( otherEntry, dependency.getTargetName() );
            }
            else
            {
                // Dependency in this project
                executeTarget( entry, dependency.getTargetName() );
            }
        }

        // Now execute the target itself
        executeTargetNoDeps( entry, name, target );

        // Mark target as complete
        entry.setTargetState( target, TargetState.FINISHED );
    }

    /**
     * Executes a target.  Does not check whether the target has been
     * executed already, and does not check that its dependencies have been
     * executed.
     *
     * @param entry the project to execute the target in.
     * @param name the name of the target.
     * @param target the target itself
     */
    private void executeTargetNoDeps( final ProjectEntry entry,
                                      final String name,
                                      final Target target )
        throws TaskException
    {
        final Project project = entry.getProject();

        // Notify listeners
        m_listenerSupport.targetStarted( project.getProjectName(), name );

        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( "exec-target.notice",
                                                  project.getProjectName(), name );
            getLogger().debug( message );
        }

        //TODO - put this back in
        //frame.getContext().setProperty( Project.TARGET, target );

        // Execute all tasks assciated with target
        final Configuration[] tasks = target.getTasks();
        for( int i = 0; i < tasks.length; i++ )
        {
            executeTask( tasks[ i ], entry.getFrame() );
        }

        // Notify listeners
        m_listenerSupport.targetFinished();
    }

    /**
     * Execute a task.
     *
     * @param task the task definition
     * @param frame the frame to execute in
     * @exception TaskException if an error occurs
     */
    private void executeTask( final Configuration task, final ExecutionFrame frame )
        throws TaskException
    {
        final String name = task.getName();

        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( "exec-task.notice", name );
            getLogger().debug( message );
        }

        //is setting name even necessary ???
        frame.getProperties().setProperty( TaskContext.NAME, name );

        //notify listeners
        m_listenerSupport.taskStarted( name );

        //run task
        m_executor.execute( task, frame );

        //notify listeners task has ended
        m_listenerSupport.taskFinished();
    }

}
