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
import java.util.Iterator;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Hierarchy;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Condition;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.executor.ExecutionFrame;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;
import org.apache.myrmidon.interfaces.model.TypeLib;
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
    implements Workspace, Composable, Parameterizable, Initializable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultWorkspace.class );

    private Executor m_executor;
    private ProjectListenerSupport m_listenerSupport = new ProjectListenerSupport();
    private ComponentManager m_componentManager;
    private Parameters m_parameters;
    private TaskContext m_baseContext;
    private HashMap m_entrys = new HashMap();
    private TypeManager m_typeManager;
    private Deployer m_deployer;
    private Hierarchy m_hierarchy;
    private int m_projectID;

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
        m_deployer = (Deployer)componentManager.lookup( Deployer.ROLE );
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

        m_hierarchy = new Hierarchy();

        final LogTargetToListenerAdapter target = new LogTargetToListenerAdapter( m_listenerSupport );
        m_hierarchy.setDefaultLogTarget( target );
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
        final TaskContext context = new DefaultTaskContext( m_componentManager );

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

    private File findTypeLib( final String libraryName )
        throws TaskException
    {
        //TODO: In future this will be expanded to allow
        //users to specify search path or automagically
        //add entries to lib path (like user specific or
        //workspace specific)
        final String name = libraryName.replace( '/', File.separatorChar ) + ".atl";

        final String home = System.getProperty( "myrmidon.home" );
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
        throws TaskException
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
                final String message = REZ.getString( "no-deploy.error", typeLib.getLibrary(), file );
                throw new TaskException( message, de );
            }
        }
    }

    private ExecutionFrame createExecutionFrame( final Project project )
        throws TaskException
    {
        //Create per frame ComponentManager
        final DefaultComponentManager componentManager =
            new DefaultComponentManager( m_componentManager );

        //Add in child type manager so each frame can register different
        //sets of tasks etc
        final TypeManager typeManager = m_typeManager.createChildTypeManager();
        componentManager.put( TypeManager.ROLE, typeManager );

        //try
        //{
        //    //Add VFS manager
        //    // TODO - need to drive this from a typelib descriptor, plus
        //    // should be adding services to the root frame, rather than here
        //    final DefaultFileSystemManager vfsManager = new DefaultFileSystemManager();
        //    vfsManager.setBaseFile( project.getBaseDirectory() );
        //    componentManager.put( FileSystemManager.ROLE, vfsManager );
        //}
        //catch( Exception e )
        //{
        //    throw new TaskException( e.getMessage(), e );
        //}

        //We need to create a new deployer so that it deploys
        //to project specific TypeManager
        final Deployer deployer;
        try
        {
            deployer = m_deployer.createChildDeployer( componentManager );
            componentManager.put( Deployer.ROLE, deployer );
        }
        catch( ComponentException e )
        {
            throw new TaskException( e.getMessage(), e );
        }

        // Deploy the imported typelibs
        deployTypeLib( deployer, project );

        //We need to place projects and ProjectManager
        //in ComponentManager so as to support project-local call()
        componentManager.put( Workspace.ROLE, this );
        componentManager.put( Project.ROLE, project );

        final String[] names = project.getProjectNames();
        for( int i = 0; i < names.length; i++ )
        {
            final String name = names[ i ];
            final Project other = project.getProject( name );
            componentManager.put( Project.ROLE + "/" + name, other );
        }

        // Create and configure the context
        final DefaultTaskContext context =
            new DefaultTaskContext( m_baseContext, componentManager );
        context.setProperty( TaskContext.BASE_DIRECTORY, project.getBaseDirectory() );

        final DefaultExecutionFrame frame = new DefaultExecutionFrame();

        try
        {
            final Logger logger =
                new LogKitLogger( m_hierarchy.getLoggerFor( "project" + m_projectID ) );
            m_projectID++;

            frame.enableLogging( logger );
            frame.contextualize( context );

            /**
             *  @todo Should no occur but done for the time being to simplify evolution.
             */
            componentManager.put( ExecutionFrame.ROLE, frame );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "bad-frame.error" );
            throw new TaskException( message, e );
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
            final String message = REZ.getString( "no-project.error", name );
            throw new TaskException( message );
        }

        return other;
    }

    /**
     * Helper method to execute a target.
     *
     * @param project the Project
     * @param targetName the name of the target
     * @param entry the context
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
            final String message = REZ.getString( "no-target.error", targetName );
            throw new TaskException( message );
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
     * @param name the name of target
     * @param target the target
     * @param frame the frame in which to execute
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
            try
            {
                final boolean result =
                    condition.evaluate( frame.getContext() );
                if( !result )
                {
                    final String message = REZ.getString( "skip-target.notice", name );
                    getLogger().debug( message );
                    return;
                }
            }
            catch( final TaskException te )
            {
                final String message = REZ.getString( "condition-eval.error", name );
                throw new TaskException( message, te );
            }
        }

        final String message = REZ.getString( "exec-target.notice", name );
        getLogger().debug( message );

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
     * @param frame the frame to execute in
     * @exception TaskException if an error occurs
     */
    private void executeTask( final Configuration task, final ExecutionFrame frame )
        throws TaskException
    {
        final String name = task.getName();

        final String message = REZ.getString( "exec-task.notice", name );
        getLogger().debug( message );

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
