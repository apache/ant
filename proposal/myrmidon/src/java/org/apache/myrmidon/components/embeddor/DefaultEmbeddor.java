/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.embeddor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.aut.converter.Converter;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.ExtensionFileFilter;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.CascadingException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.interfaces.aspect.AspectManager;
import org.apache.myrmidon.interfaces.builder.ProjectBuilder;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderManager;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.property.PropertyResolver;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.service.MultiSourceServiceManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * Default implementation of Embeddor.
 * Instantiate this to embed inside other applications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultEmbeddor
    extends AbstractLogEnabled
    implements Embeddor
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultEmbeddor.class );

    /** Package containing the default component implementations. */
    private static final String PREFIX = "org.apache.myrmidon.components.";

    private Deployer m_deployer;
    private TypeManager m_typeManager;
    private MultiSourceServiceManager m_workspaceServiceManager;

    private List m_components = new ArrayList();
    private DefaultServiceManager m_serviceManager = new DefaultServiceManager();
    private Parameters m_parameters;
    private Parameters m_defaults;

    private File m_homeDir;
    private File m_taskLibDir;
    private static final String MYRMIDON_HOME = "myrmidon.home";

    /**
     * Setup basic properties of engine.
     * Called before init() and can be used to specify alternate components in system.
     *
     * @param parameters the parameters.
     */
    public void parameterize( final Parameters parameters )
    {
        m_parameters = parameters;
    }

    /**
     * Builds a project.
     */
    public Project createProject( final String location,
                                  final String type,
                                  final Parameters parameters )
        throws Exception
    {
        try
        {
            String projectType = type;
            if( null == projectType )
            {
                projectType = FileUtil.getExtension( location );
            }

            final ProjectBuilder builder = getProjectBuilder( projectType, parameters );
            return builder.build( location );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "create-project.error", location );
            throw new CascadingException( message, e );
        }
    }

    /**
     * Creates a project builder for a project type.
     */
    private ProjectBuilder getProjectBuilder( final String type,
                                              final Parameters parameters )
        throws Exception
    {
        final TypeFactory factory = m_typeManager.getFactory( ProjectBuilder.ROLE );
        final ProjectBuilder builder = (ProjectBuilder)factory.create( type );
        setupObject( builder, m_workspaceServiceManager, parameters );
        return builder;
    }

    /**
     * Creates a workspace.
     */
    public Workspace createWorkspace( final Parameters parameters )
        throws Exception
    {
        final Workspace workspace =
            (Workspace)createService( Workspace.class, PREFIX + "workspace.DefaultWorkspace" );
        // TODO - don't do this; need some way to pass separate sets of defines and config
        // to the workspace
        parameters.setParameter( MYRMIDON_HOME, m_parameters.getParameter( MYRMIDON_HOME ) );
        setupObject( workspace, m_workspaceServiceManager, parameters );
        return workspace;
    }

    /**
     * Creates a project listener.
     *
     * @param name The shorthand name of the listener.
     * @return the listener.
     */
    public ProjectListener createListener( String name )
        throws Exception
    {
        final TypeFactory factory = m_typeManager.getFactory( ProjectListener.ROLE );
        return (ProjectListener)factory.create( name );
    }

    /**
     * Initialize the system.
     *
     * @exception Exception if an error occurs
     */
    public void initialize()
        throws Exception
    {
        // setup default properties
        m_defaults = createDefaultParameters();

        // setup the root components
        setupComponents();

        // locate the components we need
        m_deployer = (Deployer)m_serviceManager.lookup( Deployer.ROLE );
        m_typeManager = (TypeManager)m_serviceManager.lookup( TypeManager.ROLE );

        // setup a service manager that creates the project services
        final ServiceManager projServiceManager
            = (ServiceManager)createService( ServiceManager.class, PREFIX + "service.InstantiatingServiceManager" );
        setupObject( projServiceManager, m_serviceManager, m_parameters );

        // setup a service manager to be used by workspaces
        m_workspaceServiceManager = new MultiSourceServiceManager();
        m_workspaceServiceManager.add( projServiceManager );
        m_workspaceServiceManager.add( m_serviceManager );

        // setup
        setupFiles();
    }

    public void start()
        throws Exception
    {
        // Deploy all type libraries found in the classpath
        final ClassLoader libClassloader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( libClassloader );
        typeDeployer.deployAll();

        // Deploy all type libraries in the lib directory
        final ExtensionFileFilter filter = new ExtensionFileFilter( ".atl" );
        deployFromDirectory( m_deployer, m_taskLibDir, filter );
    }

    /**
     * Stops the engine.
     */
    public void stop()
    {
        //TODO - Undeploy all the tasks by killing ExecutionFrame???
    }

    /**
     * Dispose engine.
     */
    public void dispose()
    {
        // Dispose any disposable components
        for( Iterator iterator = m_components.iterator(); iterator.hasNext(); )
        {
            Object component = iterator.next();
            if( component instanceof Disposable )
            {
                final Disposable disposable = (Disposable)component;
                disposable.dispose();
            }
        }

        // Ditch everything
        m_components = null;
        m_deployer = null;
        m_serviceManager = null;
        m_parameters = null;
        m_defaults = null;
        m_homeDir = null;
        m_taskLibDir = null;
    }

    /**
     * Create default properties which includes default names of all components.
     * Overide this in sub-classes to change values.
     *
     * @return the Parameters
     */
    private Parameters createDefaultParameters()
    {
        final Parameters defaults = new Parameters();

        //create all the default properties for files/directories
        defaults.setParameter( "myrmidon.bin.path", "bin" );
        defaults.setParameter( "myrmidon.lib.path", "lib" );

        return defaults;
    }

    /**
     * Create all required components.
     */
    private void setupComponents()
        throws Exception
    {
        // Create the components
        createComponent( ExtensionManager.class, PREFIX + "extensions.DefaultExtensionManager" );
        final Object converter =
            createComponent( Converter.class, PREFIX + "converter.DefaultMasterConverter" );
        m_serviceManager.put( ConverterRegistry.ROLE, converter );
        createComponent( Configurer.class, PREFIX + "configurer.DefaultConfigurer" );
        createComponent( TypeManager.class, PREFIX + "type.DefaultTypeManager" );
        createComponent( RoleManager.class, PREFIX + "role.DefaultRoleManager" );
        createComponent( AspectManager.class, PREFIX + "aspect.DefaultAspectManager" );
        createComponent( Deployer.class, PREFIX + "deployer.DefaultDeployer" );
        createComponent( ClassLoaderManager.class, PREFIX + "classloader.DefaultClassLoaderManager" );
        createComponent( Executor.class, PREFIX + "executor.AspectAwareExecutor" );
        createComponent( PropertyResolver.class, PREFIX + "property.DefaultPropertyResolver" );

        m_serviceManager.put( Embeddor.ROLE, this );

        // Setup the components
        for( Iterator iterator = m_components.iterator(); iterator.hasNext(); )
        {
            final Object component = iterator.next();
            setupObject( component, m_serviceManager, m_parameters );
        }
    }

    /**
     * Creates a component.
     */
    private Object createComponent( final Class roleType,
                                  final String defaultImpl )
        throws Exception
    {
        final Object component = createService( roleType, defaultImpl );
        m_serviceManager.put( roleType.getName(), component );
        m_components.add( component );
        return component;
    }

    /**
     * Setup all the files attributes.
     */
    private void setupFiles()
        throws Exception
    {
        String filepath = null;

        filepath = getParameter( MYRMIDON_HOME );
        m_homeDir = ( new File( filepath ) ).getAbsoluteFile();
        checkDirectory( m_homeDir, "home-dir.name" );

        filepath = getParameter( "myrmidon.lib.path" );
        m_taskLibDir = resolveDirectory( filepath, "task-lib-dir.name" );
    }

    /**
     * Retrieve value of named property.
     * First access passed in properties and then the default properties.
     *
     * @param name the name of property
     * @return the value of property or null
     */
    private String getParameter( final String name )
    {
        String value = m_parameters.getParameter( name, null );

        if( null == value )
        {
            value = m_defaults.getParameter( name, null );
        }

        return value;
    }

    /**
     * Resolve a directory relative to another base directory.
     *
     * @param dir the base directory
     * @param name the relative directory
     * @return the created File
     * @exception Exception if an error occurs
     */
    private File resolveDirectory( final String dir, final String name )
        throws Exception
    {
        final File file = FileUtil.resolveFile( m_homeDir, dir );
        checkDirectory( file, name );
        return file;
    }

    /**
     * Verify file is a directory else throw an exception.
     *
     * @param file the File
     * @param name the name of file type (used in error messages)
     */
    private void checkDirectory( final File file, final String name )
        throws Exception
    {
        if( !file.exists() )
        {
            final String nameStr = REZ.getString( name );
            final String message = REZ.getString( "file-no-exist.error", nameStr, file );
            throw new Exception( message );
        }
        else if( !file.isDirectory() )
        {
            final String nameStr = REZ.getString( name );
            final String message = REZ.getString( "file-not-dir.error", nameStr, file );
            throw new Exception( message );
        }
    }

    /**
     * Create a component that implements an interface.
     *
     * @param roleType the name of interface/type
     * @param defaultImpl the classname of the default implementation
     * @return the created object
     * @exception Exception if an error occurs
     */
    private Object createService( final Class roleType, final String defaultImpl )
        throws Exception
    {
        final String role = roleType.getName();
        final String className = m_parameters.getParameter( role, defaultImpl );

        try
        {
            final Object object = Class.forName( className ).newInstance();

            if( !roleType.isInstance( object ) )
            {
                final String message = REZ.getString( "bad-type.error", className, roleType.getName() );
                throw new Exception( message );
            }

            return object;
        }
        catch( final IllegalAccessException iae )
        {
            final String message = REZ.getString( "bad-ctor.error", roleType.getName(), className );
            throw new Exception( message );
        }
        catch( final InstantiationException ie )
        {
            final String message =
                REZ.getString( "no-instantiate.error", roleType.getName(), className );
            throw new Exception( message );
        }
        catch( final ClassNotFoundException cnfe )
        {
            final String message =
                REZ.getString( "no-class.error", roleType.getName(), className );
            throw new Exception( message );
        }
    }

    /**
     * Sets-up an object by running it through the log-enable, compose,
     * parameterise and initialise lifecycle stages.
     */
    private void setupObject( final Object object,
                              final ServiceManager serviceManager,
                              final Parameters parameters )
        throws Exception
    {
        setupLogger( object );

        if( object instanceof Serviceable )
        {
            ( (Serviceable)object ).service( serviceManager );
        }

        if( object instanceof Parameterizable )
        {
            ( (Parameterizable)object ).parameterize( parameters );
        }

        if( object instanceof Initializable )
        {
            ( (Initializable)object ).initialize();
        }
    }

    /**
     * Deploys all type libraries in a directory.
     */
    private void deployFromDirectory( final Deployer deployer,
                                      final File directory,
                                      final FilenameFilter filter )
        throws DeploymentException
    {
        final File[] files = directory.listFiles( filter );

        if( null != files )
        {
            deployFiles( deployer, files );
        }
    }

    /**
     * Deploys a set of type libraries.
     */
    private void deployFiles( final Deployer deployer, final File[] files )
        throws DeploymentException
    {
        for( int i = 0; i < files.length; i++ )
        {
            final String filename = files[ i ].getName();

            int index = filename.lastIndexOf( '.' );
            if( -1 == index )
            {
                index = filename.length();
            }

            try
            {
                final File file = files[ i ].getCanonicalFile();
                final TypeDeployer typeDeployer = deployer.createDeployer( file );
                typeDeployer.deployAll();
            }
            catch( final DeploymentException de )
            {
                throw de;
            }
            catch( final Exception e )
            {
                final String message = REZ.getString( "bad-filename.error", files[ i ] );
                throw new DeploymentException( message, e );
            }
        }
    }
}
