/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.embeddor;

import java.io.File;
import java.io.FilenameFilter;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.ExtensionFileFilter;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.api.JavaVersion;
import org.apache.myrmidon.interfaces.aspect.AspectManager;
import org.apache.myrmidon.interfaces.builder.ProjectBuilder;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.workspace.Workspace;

/**
 * Default implementation of Embeddor.
 * Instantiate this to embed inside other applications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultEmbeddor
    extends AbstractLogEnabled
    implements Embeddor
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultEmbeddor.class );

    private Deployer m_deployer;
    private RoleManager m_roleManager;

    private AspectManager m_aspectManager;
    private TypeManager m_typeManager;
    private MasterConverter m_converter;
    private ConverterRegistry m_converterRegistry;
    private ExtensionManager m_extensionManager;

    private Executor m_executor;
    private Configurer m_configurer;

    private DefaultComponentManager m_componentManager;
    private Parameters m_parameters;
    private Parameters m_defaults;

    private File m_homeDir;
    private File m_binDir;
    private File m_libDir;
    private File m_taskLibDir;

    /**
     * Setup basic properties of engine.
     * Called before init() and can be used to specify alternate components in system.
     *
     * @param properties the properties
     */
    public void parameterize( final Parameters parameters )
    {
        m_parameters = parameters;
    }

    public Project createProject( final String location,
                                  String type,
                                  final Parameters parameters )
        throws Exception
    {
        if( null == type )
        {
            type = guessTypeFor( location );
        }

        final ProjectBuilder builder = getProjectBuilder( type, parameters );
        return builder.build( location );
    }

    private String guessTypeFor( final String location )
    {
        return FileUtil.getExtension( location );
    }

    private ProjectBuilder getProjectBuilder( final String type,
                                              final Parameters parameters )
        throws Exception
    {

        final TypeFactory factory = m_typeManager.getFactory( ProjectBuilder.ROLE );
        final ProjectBuilder builder = (ProjectBuilder)factory.create( type );

        setupLogger( builder );

        if( builder instanceof Composable )
        {
            ( (Composable)builder ).compose( m_componentManager );
        }

        if( builder instanceof Parameterizable )
        {
            ( (Parameterizable)builder ).parameterize( parameters );
        }

        if( builder instanceof Initializable )
        {
            ( (Initializable)builder ).initialize();
        }

        return builder;
    }

    public Workspace createWorkspace( final Parameters parameters )
        throws Exception
    {
        final String component = getParameter( Workspace.ROLE );
        final Workspace workspace =
            (Workspace)createComponent( component, Workspace.class );

        setupLogger( workspace );

        if( workspace instanceof Composable )
        {
            ( (Composable)workspace ).compose( m_componentManager );
        }

        if( workspace instanceof Parameterizable )
        {
            ( (Parameterizable)workspace ).parameterize( parameters );
        }

        if( workspace instanceof Initializable )
        {
            ( (Initializable)workspace ).initialize();
        }

        return workspace;
    }

    /**
     * Initialize the system.
     *
     * @exception Exception if an error occurs
     */
    public void initialize()
        throws Exception
    {
        //setup default properties
        m_defaults = createDefaultParameters();

        //create all the components
        createComponents();

        //setup the component manager
        m_componentManager = createComponentManager();

        setupComponents();

        setupFiles();
    }

    public void start()
        throws Exception
    {
        final ExtensionFileFilter filter = new ExtensionFileFilter( ".atl" );
        deployFromDirectory( m_deployer, m_taskLibDir, filter );
    }

    public void stop()
    {
        //Undeploy all the tasks by killing ExecutionFrame???
    }

    /**
     * Dispose engine.
     *
     * @exception Exception if an error occurs
     */
    public void dispose()
    {
        m_extensionManager = null;
        m_aspectManager = null;
        m_roleManager = null;
        m_converterRegistry = null;
        m_converter = null;
        m_executor = null;
        m_deployer = null;
        m_configurer = null;
        m_componentManager = null;
        m_parameters = null;
        m_defaults = null;
        m_homeDir = null;
        m_binDir = null;
        m_libDir = null;
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

        //create all the default properties for components
        final String PREFIX = "org.apache.myrmidon.components.";
        defaults.setParameter( AspectManager.ROLE, PREFIX + "aspect.DefaultAspectManager" );
        defaults.setParameter( RoleManager.ROLE, PREFIX + "role.DefaultRoleManager" );
        defaults.setParameter( MasterConverter.ROLE, PREFIX + "converter.DefaultMasterConverter" );
        defaults.setParameter( ConverterRegistry.ROLE, PREFIX + "converter.DefaultConverterRegistry" );
        defaults.setParameter( TypeManager.ROLE, PREFIX + "type.DefaultTypeManager" );
        defaults.setParameter( Executor.ROLE,
                               //"org.apache.myrmidon.components.executor.DefaultExecutor" );
                               //"org.apache.myrmidon.components.executor.PrintingExecutor" );
                               PREFIX + "executor.AspectAwareExecutor" );
        defaults.setParameter( Workspace.ROLE, PREFIX + "workspace.DefaultWorkspace" );
        defaults.setParameter( Deployer.ROLE, PREFIX + "deployer.DefaultDeployer" );
        defaults.setParameter( Configurer.ROLE, PREFIX + "configurer.DefaultConfigurer" );
        defaults.setParameter( ExtensionManager.ROLE, PREFIX + "extensions.DefaultExtensionManager" );

        return defaults;
    }

    /**
     * Create a ComponentManager containing all components in engine.
     *
     * @return the ComponentManager
     */
    private DefaultComponentManager createComponentManager()
    {
        final DefaultComponentManager componentManager = new DefaultComponentManager();

        componentManager.put( MasterConverter.ROLE, m_converter );

        //Following components required when Myrmidon is used as build tool
        componentManager.put( Embeddor.ROLE, this );

        //Following components required when Myrmidon allows user deployment of tasks etal.
        componentManager.put( RoleManager.ROLE, m_roleManager );
        componentManager.put( Deployer.ROLE, m_deployer );
        componentManager.put( ExtensionManager.ROLE, m_extensionManager );

        //Following components used when want to types (ie tasks/mappers etc)
        componentManager.put( TypeManager.ROLE, m_typeManager );
        componentManager.put( ConverterRegistry.ROLE, m_converterRegistry );

        componentManager.put( AspectManager.ROLE, m_aspectManager );

        //Following components required when allowing Container tasks
        componentManager.put( Configurer.ROLE, m_configurer );
        componentManager.put( Executor.ROLE, m_executor );

        return componentManager;
    }

    /**
     * Create all required components.
     *
     * @exception Exception if an error occurs
     */
    private void createComponents()
        throws Exception
    {
        String component = null;

        component = getParameter( ConverterRegistry.ROLE );
        m_converterRegistry = (ConverterRegistry)createComponent( component, ConverterRegistry.class );

        component = getParameter( ExtensionManager.ROLE );
        m_extensionManager = (ExtensionManager)createComponent( component, ExtensionManager.class );

        component = getParameter( MasterConverter.ROLE );
        m_converter = (MasterConverter)createComponent( component, MasterConverter.class );

        component = getParameter( Configurer.ROLE );
        m_configurer = (Configurer)createComponent( component, Configurer.class );

        component = getParameter( TypeManager.ROLE );
        m_typeManager = (TypeManager)createComponent( component, TypeManager.class );

        component = getParameter( RoleManager.ROLE );
        m_roleManager = (RoleManager)createComponent( component, RoleManager.class );

        component = getParameter( AspectManager.ROLE );
        m_aspectManager = (AspectManager)createComponent( component, AspectManager.class );

        component = getParameter( Deployer.ROLE );
        m_deployer = (Deployer)createComponent( component, Deployer.class );

        component = getParameter( Executor.ROLE );
        m_executor = (Executor)createComponent( component, Executor.class );
    }

    /**
     * Setup all the components. (ir run all required lifecycle methods).
     *
     * @exception Exception if an error occurs
     */
    private void setupComponents()
        throws Exception
    {
        setupComponent( m_extensionManager );
        setupComponent( m_roleManager );
        setupComponent( m_aspectManager );
        setupComponent( m_converterRegistry );
        setupComponent( m_converter );
        setupComponent( m_executor );
        setupComponent( m_deployer );
        setupComponent( m_configurer );
    }

    /**
     * Setup an individual component.
     *
     * @param component the component
     * @exception Exception if an error occurs
     */
    private void setupComponent( final Component component )
        throws Exception
    {
        setupLogger( component );

        if( component instanceof Composable )
        {
            ( (Composable)component ).compose( m_componentManager );
        }

        if( component instanceof Parameterizable )
        {
            ( (Parameterizable)component ).parameterize( m_parameters );
        }

        if( component instanceof Initializable )
        {
            ( (Initializable)component ).initialize();
        }
    }

    /**
     * Setup all the files attributes.
     */
    private void setupFiles()
        throws Exception
    {
        String filepath = null;

        filepath = getParameter( "myrmidon.home" );
        m_homeDir = ( new File( filepath ) ).getAbsoluteFile();
        checkDirectory( m_homeDir, "home" );

        filepath = getParameter( "myrmidon.bin.path" );
        m_binDir = resolveDirectory( filepath, "bin-dir" );

        filepath = getParameter( "myrmidon.lib.path" );
        m_taskLibDir = resolveDirectory( filepath, "task-lib-dir" );
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
            final String message = REZ.getString( "file-no-exist.error", name, file );
            throw new Exception( message );
        }
        else if( !file.isDirectory() )
        {
            final String message = REZ.getString( "file-not-dir.error", name, file );
            throw new Exception( message );
        }
    }

    /**
     * Helper method to retrieve current JVM version.
     *
     * @return the current JVM version
     */
    private JavaVersion getJavaVersion()
    {
        JavaVersion version = JavaVersion.JAVA1_0;

        try
        {
            Class.forName( "java.lang.Void" );
            version = JavaVersion.JAVA1_1;
            Class.forName( "java.lang.ThreadLocal" );
            version = JavaVersion.JAVA1_2;
            Class.forName( "java.lang.StrictMath" );
            version = JavaVersion.JAVA1_3;
        }
        catch( final ClassNotFoundException cnfe )
        {
        }

        return version;
    }

    /**
     * Create a component that implements an interface.
     *
     * @param component the name of the component
     * @param clazz the name of interface/type
     * @return the created object
     * @exception Exception if an error occurs
     */
    private Object createComponent( final String component, final Class clazz )
        throws Exception
    {
        try
        {
            final Object object = Class.forName( component ).newInstance();

            if( !clazz.isInstance( object ) )
            {
                final String message = REZ.getString( "bad-type.error", component, clazz.getName() );
                throw new Exception( message );
            }

            return object;
        }
        catch( final IllegalAccessException iae )
        {
            final String message = REZ.getString( "bad-ctor.error", clazz.getName(), component );
            throw new Exception( message );
        }
        catch( final InstantiationException ie )
        {
            final String message =
                REZ.getString( "no-instantiate.error", clazz.getName(), component );
            throw new Exception( message );
        }
        catch( final ClassNotFoundException cnfe )
        {
            final String message =
                REZ.getString( "no-class.error", clazz.getName(), component );
            throw new Exception( message );
        }
    }

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

    private void deployFiles( final Deployer deployer, final File[] files )
        throws DeploymentException
    {
        for( int i = 0; i < files.length; i++ )
        {
            final String filename = files[ i ].getName();

            int index = filename.lastIndexOf( '.' );
            if( -1 == index ) index = filename.length();

            final String name = filename.substring( 0, index );

            try
            {
                final File file = files[ i ].getCanonicalFile();
                deployer.deploy( file );
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
