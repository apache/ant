/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.runtime;

import java.io.File;
import java.util.Properties;
import org.apache.ant.AntException;
import org.apache.ant.configuration.Configurer;
import org.apache.ant.convert.engine.ConverterEngine;
import org.apache.ant.project.ProjectBuilder;
import org.apache.ant.project.ProjectEngine;
import org.apache.myrmidon.api.JavaVersion;
import org.apache.ant.tasklet.engine.DataTypeEngine;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.ant.tasklet.engine.TskDeployer;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.camelot.CamelotUtil;
import org.apache.avalon.framework.camelot.DefaultFactory;
import org.apache.avalon.framework.camelot.Deployer;
import org.apache.avalon.framework.camelot.Factory;
import org.apache.avalon.excalibur.io.FileUtil;

/**
 * Default implementation of Ant runtime.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultAntEngine
    extends AbstractLoggable
    implements AntEngine, Initializable
{
    protected ConverterEngine          m_converterEngine;
    protected DataTypeEngine           m_dataTypeEngine;
    protected TaskletEngine            m_taskletEngine;
    protected ProjectEngine            m_projectEngine;

    protected ProjectBuilder           m_builder;
    protected TskDeployer              m_deployer;
    protected Configurer               m_configurer;

    protected Factory                  m_factory;

    protected DefaultComponentManager  m_componentManager;
    protected Properties               m_properties;
    protected Properties               m_defaults;

    protected File                     m_homeDir;
    protected File                     m_binDir;
    protected File                     m_libDir;
    protected File                     m_taskLibDir;
       
    /**
     * Setup basic properties of engine. 
     * Called before init() and can be used to specify alternate components in system.
     *
     * @param properties the properties
     */
    public void setProperties( final Properties properties )
    {
        m_properties = properties;
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
    public ProjectEngine getProjectEngine()
    {
        return m_projectEngine;
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
        m_defaults = createDefaultProperties();

        //create all the components
        m_factory = new DefaultFactory();
        createComponents();

        //setup the component manager
        m_componentManager = createComponentManager();

        setupComponents();

        setupFiles();

        CamelotUtil.deployFromDirectory( m_deployer, m_taskLibDir, ".tsk" );
    }

    /**
     * Dispose engine.
     *
     * @exception Exception if an error occurs
     */
    public void dispose()
        throws Exception
    {
        m_converterEngine = null;
        m_dataTypeEngine = null;
        m_taskletEngine = null;
        m_projectEngine = null;
        m_builder = null;
        m_deployer = null;
        m_configurer = null;
        m_factory = null;
        m_componentManager = null;
        m_properties = null;
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
     * @return the Properties
     */
    protected Properties createDefaultProperties()
    {
        final Properties defaults = new Properties();

        //create all the default properties for files/directories
        defaults.setProperty( "ant.path.bin", "bin" );
        defaults.setProperty( "ant.path.lib", "lib" );
        defaults.setProperty( "ant.path.task-lib", "lib" );

        //create all the default properties for components
        defaults.setProperty( "ant.comp.converter",
                              "org.apache.ant.convert.engine.DefaultConverterEngine" );
        defaults.setProperty( "ant.comp.datatype",
                              "org.apache.ant.tasklet.engine.DefaultDataTypeEngine" );
        defaults.setProperty( "ant.comp.tasklet",
                              "org.apache.ant.tasklet.engine.DefaultTaskletEngine" );
        defaults.setProperty( "ant.comp.project",
                              "org.apache.ant.project.DefaultProjectEngine" );
        defaults.setProperty( "ant.comp.builder",
                              "org.apache.ant.project.DefaultProjectBuilder" );
        defaults.setProperty( "ant.comp.deployer",
                              "org.apache.ant.tasklet.engine.DefaultTskDeployer" );
        defaults.setProperty( "ant.comp.configurer",
                              "org.apache.ant.configuration.DefaultConfigurer" );

        return defaults;
    }

    /**
     * Create a ComponentManager containing all components in engine.
     *
     * @return the ComponentManager
     */
    protected DefaultComponentManager createComponentManager()
    {
        final DefaultComponentManager componentManager = new DefaultComponentManager();

        componentManager.put( "org.apache.ant.tasklet.engine.TaskletEngine", m_taskletEngine );
        componentManager.put( "org.apache.ant.project.ProjectEngine", m_projectEngine );
        componentManager.put( "org.apache.ant.convert.engine.ConverterEngine", 
                              m_converterEngine );
        componentManager.put( "org.apache.ant.convert.Converter", m_converterEngine );
        componentManager.put( "org.apache.ant.tasklet.engine.DataTypeEngine", m_dataTypeEngine );
        componentManager.put( "org.apache.ant.project.ProjectBuilder", m_builder );
        componentManager.put( "org.apache.ant.tasklet.engine.TskDeployer", m_deployer );
        componentManager.put( "org.apache.avalon.framework.camelot.Factory", m_factory );
        componentManager.put( "org.apache.ant.configuration.Configurer", m_configurer );

        return componentManager;
    }

    /**
     * Create all required components.
     *
     * @exception Exception if an error occurs
     */
    protected void createComponents()
        throws Exception
    {
        String component = null;

        component = getProperty( "ant.comp.converter" );
        m_converterEngine = (ConverterEngine)createComponent( component, ConverterEngine.class );

        component = getProperty( "ant.comp.datatype" );
        m_dataTypeEngine = (DataTypeEngine)createComponent( component, DataTypeEngine.class );
        
        component = getProperty( "ant.comp.tasklet" );
        m_taskletEngine = (TaskletEngine)createComponent( component, TaskletEngine.class );
        
        component = getProperty( "ant.comp.project" );
        m_projectEngine = (ProjectEngine)createComponent( component, ProjectEngine.class );

        component = getProperty( "ant.comp.builder" );
        m_builder =(ProjectBuilder)createComponent( component, ProjectBuilder.class );

        component = getProperty( "ant.comp.deployer" );
        m_deployer = (TskDeployer)createComponent( component, TskDeployer.class );

        component = getProperty( "ant.comp.configurer" );
        m_configurer = (Configurer)createComponent( component, Configurer.class );
    }

    /**
     * Setup all the components. (ir run all required lifecycle methods).
     *
     * @exception Exception if an error occurs
     */
    protected void setupComponents()
        throws Exception
    {
        setupComponent( m_factory );
        setupComponent( m_converterEngine );
        setupComponent( m_dataTypeEngine );
        setupComponent( m_taskletEngine );
        setupComponent( m_projectEngine );
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
    protected void setupComponent( final Component component )
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
    protected void setupFiles()
    {
        String filepath = null;

        filepath = getProperty( "ant.home" );
        m_homeDir = (new File( filepath )).getAbsoluteFile();
        checkDirectory( m_homeDir, "ant-home" );

        filepath = getProperty( "ant.path.bin" );
        m_binDir = resolveDirectory( filepath, "bin-dir" );
        
        filepath = getProperty( "ant.path.lib" );
        m_libDir = resolveDirectory( filepath, "lib-dir" );

        filepath = getProperty( "ant.path.task-lib" );
        m_taskLibDir = resolveDirectory( filepath, "task-lib-dir" );
    }

    /**
     * Retrieve value of named property. 
     * First access passed in properties and then the default properties.
     *
     * @param name the name of property
     * @return the value of property or null
     */
    protected String getProperty( final String name )
    {
        String value = m_properties.getProperty( name );

        if( null == value )
        {
            value = m_defaults.getProperty( name );
        }

        return value;
    }

    /**
     * Resolve a directory relative to another base directory.
     *
     * @param dir the base directory
     * @param name the relative directory
     * @return the created File
     * @exception AntException if an error occurs
     */
    protected File resolveDirectory( final String dir, final String name )
        throws AntException
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
    protected void checkDirectory( final File file, final String name )
        throws AntException
    { 
        if( !file.exists() )
        {
            throw new AntException( name + " (" + file + ") does not exist" );
        }
        else if( !file.isDirectory() )
        {
            throw new AntException( name + " (" + file + ") is not a directory" );
        }
    }

    /**
     * Helper method to retrieve current JVM version.
     * Basically stolen from original Ant sources.
     *
     * @return the current JVM version
     */
    protected JavaVersion getJavaVersion()
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
     * @exception AntException if an error occurs
     */
    protected Object createComponent( final String component, final Class clazz )
        throws AntException
    {
        try
        {
            final Object object = Class.forName( component ).newInstance();

            if( !clazz.isInstance( object ) )
            {
                throw new AntException( "Object " + component + " is not an instance of " + 
                                        clazz );
            }

            return object;
        }
        catch( final IllegalAccessException iae )
        { 
            throw new AntException( "Non-public constructor for " + clazz + " " + component, 
                                    iae );
        }
        catch( final InstantiationException ie )
        {
            throw new AntException( "Error instantiating class for " + clazz + " " + component, 
                                    ie );
        }
        catch( final ClassNotFoundException cnfe )
        {
            throw new AntException( "Could not find the class for " + clazz + 
                                    " (" + component + ")", cnfe );
        }
    }
}
