/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.embeddor;

import java.io.File;
import org.apache.myrmidon.components.converter.MasterConverter;
import org.apache.myrmidon.components.converter.ConverterRegistry;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.camelot.CamelotUtil;
import org.apache.avalon.framework.camelot.DefaultFactory;
import org.apache.avalon.framework.camelot.Deployer;
import org.apache.avalon.framework.camelot.Factory;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.api.JavaVersion;
import org.apache.myrmidon.components.builder.ProjectBuilder;
import org.apache.myrmidon.components.configurer.Configurer;
import org.apache.myrmidon.components.deployer.TskDeployer;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.manager.ProjectManager;
import org.apache.myrmidon.components.type.TypeManager;

/**
 * Default implementation of Embeddor.
 * Instantiate this to embed inside other applications.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class MyrmidonEmbeddor
    extends AbstractLoggable
    implements Embeddor
{
    private ProjectManager           m_projectManager;
    private ProjectBuilder           m_builder;
    private TskDeployer              m_deployer;

    private TypeManager              m_typeManager;
    private MasterConverter          m_converter;
    private ConverterRegistry        m_converterRegistry;

    private Executor                 m_executor;
    private Configurer               m_configurer;


    private DefaultComponentManager  m_componentManager;
    private Parameters               m_parameters;
    private Parameters               m_defaults;

    private File                     m_homeDir;
    private File                     m_binDir;
    private File                     m_libDir;
    private File                     m_taskLibDir;

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

    /**
     * Retrieve builder for runtime.
     * Valid after init() call
     *
     * @return the ProjectBuilder
     */
    public ProjectBuilder getProjectBuilder()
    {
        return m_builder;
    }

    /**
     * Retrieve project engine for runtime.
     * Valid after init() call
     *
     * @return the ProjectBuilder
     */
    public ProjectManager getProjectManager()
    {
        return m_projectManager;
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
        CamelotUtil.deployFromDirectory( m_deployer, m_taskLibDir, ".tsk" );
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
        throws Exception
    {
        m_converterRegistry = null;
        m_converter = null;
        m_executor = null;
        m_projectManager = null;
        m_builder = null;
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
        defaults.setParameter( MasterConverter.ROLE,
                               "org.apache.myrmidon.components.converter.DefaultMasterConverter" );
        defaults.setParameter( ConverterRegistry.ROLE,
                               "org.apache.myrmidon.components.converter.DefaultConverterRegistry" );
        defaults.setParameter( TypeManager.ROLE,
                               "org.apache.myrmidon.components.type.DefaultTypeManager" );
        defaults.setParameter( Executor.ROLE,
                               "org.apache.myrmidon.components.executor.DefaultExecutor" );
        defaults.setParameter( ProjectManager.ROLE,
                               "org.apache.myrmidon.components.manager.DefaultProjectManager" );
        defaults.setParameter( ProjectBuilder.ROLE,
                               "org.apache.myrmidon.components.builder.DefaultProjectBuilder" );
        defaults.setParameter( TskDeployer.ROLE,
                               "org.apache.myrmidon.components.deployer.DefaultTskDeployer" );
        defaults.setParameter( Configurer.ROLE,
                               "org.apache.myrmidon.components.configurer.DefaultConfigurer" );

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
        componentManager.put( ProjectManager.ROLE, m_projectManager );
        componentManager.put( ProjectBuilder.ROLE, m_builder );

        //Following components required when Myrmidon allows user deployment of tasks etal.
        componentManager.put( TskDeployer.ROLE, m_deployer );

        //Following components used when want to types (ie tasks/mappers etc)
        componentManager.put( TypeManager.ROLE, m_typeManager );
        componentManager.put( ConverterRegistry.ROLE, m_converterRegistry );

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

        component = getParameter( "org.apache.myrmidon.components.converter.MasterConverter" );
        m_converter = (MasterConverter)createComponent( component, MasterConverter.class );

        component = getParameter( Configurer.ROLE );
        m_configurer = (Configurer)createComponent( component, Configurer.class );

        component = getParameter( TypeManager.ROLE );
        m_typeManager = (TypeManager)createComponent( component, TypeManager.class );

        component = getParameter( TskDeployer.ROLE );
        m_deployer = (TskDeployer)createComponent( component, TskDeployer.class );

        component = getParameter( Executor.ROLE );
        m_executor = (Executor)createComponent( component, Executor.class );

        component = getParameter( ProjectManager.ROLE );
        m_projectManager = (ProjectManager)createComponent( component, ProjectManager.class );

        component = getParameter( ProjectBuilder.ROLE );
        m_builder =(ProjectBuilder)createComponent( component, ProjectBuilder.class );
    }

    /**
     * Setup all the components. (ir run all required lifecycle methods).
     *
     * @exception Exception if an error occurs
     */
    private void setupComponents()
        throws Exception
    {
        setupComponent( m_converterRegistry );
        setupComponent( m_converter );
        setupComponent( m_executor );
        setupComponent( m_projectManager );
        setupComponent( m_builder );
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
            ((Composable)component).compose( m_componentManager );
        }

        if( component instanceof Initializable )
        {
            ((Initializable)component).initialize();
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
        m_homeDir = (new File( filepath )).getAbsoluteFile();
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
            throw new Exception( name + " (" + file + ") does not exist" );
        }
        else if( !file.isDirectory() )
        {
            throw new Exception( name + " (" + file + ") is not a directory" );
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
        catch( final ClassNotFoundException cnfe ) {}

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
                throw new Exception( "Object " + component + " is not an instance of " +
                                     clazz );
            }

            return object;
        }
        catch( final IllegalAccessException iae )
        {
            throw new Exception( "Non-public constructor for " + clazz + " " + component );
        }
        catch( final InstantiationException ie )
        {
            throw new Exception( "Error instantiating class for " + clazz + " " + component );
        }
        catch( final ClassNotFoundException cnfe )
        {
            throw new Exception( "Could not find the class for " + clazz +
                                 " (" + component + ")" );
        }
    }
}
