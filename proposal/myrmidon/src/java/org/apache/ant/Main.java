/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.ant.launcher.AntLoader;
import org.apache.ant.project.DefaultProjectEngine;
import org.apache.ant.project.Project;
import org.apache.ant.project.ProjectBuilder;
import org.apache.ant.project.ProjectEngine;
import org.apache.ant.project.ProjectListener;
import org.apache.ant.project.ProjectToListenerAdapter;
import org.apache.ant.tasklet.JavaVersion;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.Deployer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.util.StringUtil;
import org.apache.avalon.util.cli.AbstractMain;
import org.apache.avalon.util.cli.CLOption;
import org.apache.avalon.util.cli.CLOptionDescriptor;
import org.apache.avalon.util.io.ExtensionFileFilter;
import org.apache.avalon.util.io.FileUtil;
import org.apache.log.Category;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.log.Priority;

/**
 * The class to kick the tires and light the fires.
 * Starts ant, loads ProjectBuilder, builds project then uses ProjectEngine
 * to run project.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Main
    extends AbstractMain
{
    public final static String     BUILD_DATE                = "@@DATE@@";
    public final static String     BUILD_VERSION             = "@@VERSION@@";
    public final static String     VERSION                   = 
        "Ant " + BUILD_VERSION + " compiled on " + BUILD_DATE;

    protected final static String  DEFAULT_LOGLEVEL          = "WARN";
    protected final static String  DEFAULT_LIB_DIRECTORY     = "lib";
    protected final static String  DEFAULT_TASKLIB_DIRECTORY = DEFAULT_LIB_DIRECTORY;
    protected final static String  DEFAULT_FILENAME          = "build.xmk";

    protected final static String  DEFAULT_ENGINE            = 
        "org.apache.ant.project.DefaultProjectEngine";

    protected final static String  DEFAULT_LISTENER          = 
        "org.apache.ant.project.DefaultProjectListener";

    protected final static String  DEFAULT_BUILDER           =  
        "org.apache.ant.project.DefaultProjectBuilder";

    private static final int       HELP_OPT                  = 'h';
    private static final int       QUIET_OPT                 = 'q';
    private static final int       VERBOSE_OPT               = 'v';
    private static final int       FILE_OPT                  = 'f';
    private static final int       LOG_LEVEL_OPT             = 'l';
    private static final int       DEFINE_OPT                = 'D';
    private static final int       VERSION_OPT               = 1;
    private static final int       LISTENER_OPT              = 2;
    private static final int       BIN_DIR_OPT               = 3;
    private static final int       LIB_DIR_OPT               = 4;
    private static final int       TASKLIB_DIR_OPT           = 5;
    private static final int       INCREMENTAL_OPT           = 6;
    private static final int       HOME_DIR_OPT              = 7;
    
    //incompatable options for info options
    private static final int[]     INFO_OPT_INCOMPAT         = new int[] 
    { 
        HELP_OPT, QUIET_OPT, VERBOSE_OPT, FILE_OPT, 
        LOG_LEVEL_OPT, VERSION_OPT, LISTENER_OPT,
        DEFINE_OPT
        //BIN_DIR_OPT, LIB_DIR_OPT, TASKLIB_DIR_OPT, HOME_DIR_OPT
    };
    
    //incompatable options for other logging options
    private static final int[]     LOG_OPT_INCOMPAT          = new int[] 
    {
        QUIET_OPT, VERBOSE_OPT, LOG_LEVEL_OPT
    };

    protected Logger               m_logger;

    protected ProjectListener      m_listener;
    protected File                 m_binDir;
    protected File                 m_homeDir;
    protected File                 m_libDir;
    protected File                 m_taskLibDir;
    protected File                 m_buildFile;
    protected File                 m_userDir;

    public static void main( final String[] args )
    {
        final Main main = new Main();

        try { main.execute( args ); }
        catch( final AntException ae )
        {
            main.m_logger.error( "Error: " + ae.getMessage() );
            main.m_logger.debug( "Exception..." + StringUtil.printStackTrace( ae ) );
        }
        catch( final Throwable throwable )
        {
            main.m_logger.error( "Error: " + throwable );
            main.m_logger.debug( "Exception..." + StringUtil.printStackTrace( throwable ) );
        }
    }

    /**
     * Initialise the options for command line parser.
     */
    protected CLOptionDescriptor[] createCLOptions()
    {
        //TODO: localise
        final CLOptionDescriptor[] options = new CLOptionDescriptor[ 13 ];

        options[0] =
            new CLOptionDescriptor( "help",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    HELP_OPT,
                                    "display this help message",
                                    INFO_OPT_INCOMPAT );
        
        options[1] =
            new CLOptionDescriptor( "file",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    FILE_OPT,
                                    "the build file." );

        options[2] =
            new CLOptionDescriptor( "log-level",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LOG_LEVEL_OPT,
                                    "the verbosity level at which to log messages. " +
                                    "(DEBUG|INFO|WARN|ERROR|FATAL_ERROR)",
                                    LOG_OPT_INCOMPAT );

        options[3] =
            new CLOptionDescriptor( "quiet",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    QUIET_OPT,
                                    "equivelent to --log-level=FATAL_ERROR",
                                    LOG_OPT_INCOMPAT );

        options[4] =
            new CLOptionDescriptor( "verbose",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    VERBOSE_OPT,
                                    "equivelent to --log-level=INFO",
                                    LOG_OPT_INCOMPAT );

        options[5] =
            new CLOptionDescriptor( "listener",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LISTENER_OPT,
                                    "the listener for log events." );

        options[6] =
            new CLOptionDescriptor( "version",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    VERSION_OPT,
                                    "display version",
                                    INFO_OPT_INCOMPAT );

        options[7] =
            new CLOptionDescriptor( "bin-dir",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    BIN_DIR_OPT,
                                    "the listener for log events." );

        options[8] =
            new CLOptionDescriptor( "lib-dir",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LIB_DIR_OPT,
                                    "the lib directory to scan for jars/zip files." );

        options[9] =
            new CLOptionDescriptor( "task-lib-dir",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    TASKLIB_DIR_OPT,
                                    "the task lib directory to scan for .tsk files." );
        options[10] =
            new CLOptionDescriptor( "incremental",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    INCREMENTAL_OPT,
                                    "Run in incremental mode" );
        options[11] =
            new CLOptionDescriptor( "ant-home",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    HOME_DIR_OPT,
                                    "Specify ant home directory" );
        options[12] =
            new CLOptionDescriptor( "define",
                                    CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                                    DEFINE_OPT,
                                    "Define a variable (ie -Dfoo=var)" );
        return options;
    }

    /**
     * Entry point for standard ant.
     *
     * @param clOptions the list of command line options
     */
    protected void execute( final List clOptions )
        throws Throwable
    {
        final int size = clOptions.size();
        final ArrayList targets = new ArrayList();
        String filename = null;
        String listenerName = null;
        String builderName = null;
        String logLevel = null;
        String binDir = null;
        String homeDir = null;
        String libDir = null;
        String taskLibDir = null;
        boolean incremental = false;
        HashMap defines = new HashMap();

        for( int i = 0; i < size; i++ ) 
        {
            final CLOption option = (CLOption)clOptions.get( i );
                
            switch( option.getId() )
            {
            case 0: targets.add( option.getArgument() ); break;
            case HELP_OPT: usage(); return;
            case VERSION_OPT: System.out.println( VERSION ); return;
            case FILE_OPT: filename = option.getArgument(); break;
            case BIN_DIR_OPT: binDir = option.getArgument(); break;
            case LIB_DIR_OPT: libDir = option.getArgument(); break;
            case HOME_DIR_OPT: homeDir = option.getArgument(); break;
            case TASKLIB_DIR_OPT: taskLibDir = option.getArgument(); break;
            case VERBOSE_OPT: logLevel = "INFO"; break;
            case QUIET_OPT: logLevel = "ERROR"; break;
            case LOG_LEVEL_OPT: logLevel = option.getArgument(); break; 
            case LISTENER_OPT: listenerName = option.getArgument(); break;
            case INCREMENTAL_OPT: incremental = true; break;

            case DEFINE_OPT: 
                defines.put( option.getArgument( 0 ), option.getArgument( 1 ) );
                break;
            }
        }

        if( null == logLevel ) logLevel = getDefaultLogLevel();
        if( null == listenerName ) listenerName = getDefaultListener();
        if( null == filename ) filename = getDefaultFilename();
        if( null == libDir ) libDir = getDefaultLibDir();
        if( null == taskLibDir ) taskLibDir = getDefaultTaskLibDir();
        if( null == builderName ) builderName = getBuilderNameFor( filename );

        setupLogger( logLevel ); //handle logging...
        setupListener( listenerName ); //handle listener..
        setupDefaultAntDirs();        

        if( null == binDir && null == homeDir ) 
        {
            m_homeDir = getDefaultHomeDir();
            m_binDir = m_homeDir.getParentFile();
        }
        else if( null == binDir ) // && null != homeDir
        {
            m_homeDir = getHomeDir( homeDir );
            m_binDir = new File( m_homeDir, "bin" );
        }
        else
        {
            m_binDir = getBinDir( binDir );
            m_homeDir = m_binDir.getParentFile();
        }

        m_libDir = getLibDir( m_homeDir, libDir );
        m_taskLibDir = getTaskLibDir( m_homeDir, taskLibDir );
        m_buildFile = getFile( filename );

        m_logger.warn( "Ant Build File: " + m_buildFile );
        m_logger.info( "Ant Home Directory: " + m_homeDir );
        m_logger.info( "Ant Bin Directory: " + m_binDir );
        m_logger.debug( "Ant Lib Directory: " + m_libDir );
        m_logger.debug( "Ant Task Lib Directory: " + m_taskLibDir );

        setupContextClassLoader( m_libDir );

        final Project project = getProject( builderName, m_buildFile );
        setupProjectContext( project, defines );

        final ProjectEngine engine = getProjectEngine();

        //make sure Engine is sweet...
        if( engine instanceof Initializable )
        {
            ((Initializable)engine).init();
        }

        engine.addProjectListener( m_listener );

        deployDefaultTaskLibs( engine, m_taskLibDir );

        BufferedReader reader = null;

        while( true )
        {
            doBuild( engine, project, targets );

            if( !incremental ) break;

            System.out.println( "Continue ? (Enter no to stop)" );

            if( null == reader )
            {
                reader = new BufferedReader( new InputStreamReader( System.in ) );
            }

            String line = reader.readLine();
            
            if( line.equalsIgnoreCase( "no" ) ) break;
            
        }
        
        if( engine instanceof Disposable )
        {
            ((Disposable)engine).dispose();
        }
    }

    protected void deployDefaultTaskLibs( final ProjectEngine engine, 
                                          final File taskLibDirectory )
    
    {
        final ExtensionFileFilter filter = new ExtensionFileFilter( ".tsk" );

        final File[] files = taskLibDirectory.listFiles( filter );
        final Deployer deployer = engine.getDeployer();

        for( int i = 0; i < files.length; i++ )
        {
            final String name = files[ i ].getName();

            try
            { 
                deployer.deploy( name.substring( 0, name.length() - 4 ), 
                                 files[ i ].toURL() );
            }
            catch( final MalformedURLException mue ) {}
            catch( final DeploymentException de )
            {
                throw new AntException( "Failed to deploy task library " + files[ i ], 
                                        de );
            }
        }
    }

    protected void doBuild( final ProjectEngine engine, 
                            final Project project, 
                            final ArrayList targets )
    {
        try
        {
            final int targetCount = targets.size();
        
            if( 0 == targetCount )
            {
                engine.execute( project, project.getDefaultTargetName() );
            }
            else
            {
                for( int i = 0; i < targetCount; i++ )
                {
                    engine.execute( project, (String)targets.get( i ) );
                }
            }
        }
        catch( final AntException ae )
        {
            m_logger.error( "BUILD FAILED" );
            m_logger.error( "Reason:\n" + StringUtil.printStackTrace( ae, 5, true ) );
        }
    } 
    
    protected void setupLogger( final String logLevel )
    {
        m_logger = createLogger( logLevel );
    }

    protected void setupListener( final String listenerName )
    {
        m_listener = createListener( listenerName );
        m_logger.addLogTarget( new ProjectToListenerAdapter( m_listener ) );
    }

    protected void setupContextClassLoader( final File libDir )
    {
        setupClassLoader( libDir );
        Thread.currentThread().setContextClassLoader( AntLoader.getLoader() );
    }

    protected void setupClassLoader( final File libDir )
    {
        final ExtensionFileFilter filter = 
            new ExtensionFileFilter( new String[] { ".jar", ".zip" } );

        final File[] files = libDir.listFiles( filter );

        final AntLoader classLoader = AntLoader.getLoader();

        for( int i = 0; i < files.length; i++ )
        {
            if( !files[ i ].getName().equals( "ant.jar" ) &&
                !files[ i ].getName().equals( "myrmidon.jar" ) &&
                !files[ i ].getName().equals( "avalonapi.jar" ) )
            {                
                try { classLoader.addURL( files[ i ].toURL() ); }
                catch( final MalformedURLException mue ) {}
            }
        }        
    }

    protected Project getProject( final String builderName, final File file )
        throws AntException, IOException
    {
        m_logger.debug( "Ant Project Builder: " + builderName );
        final ProjectBuilder builder = createBuilder( builderName );
        builder.setLogger( m_logger );

        //create the project
        final Project project = builder.build( file );

        return project;
    }

    protected void setupProjectContext( final Project project, final HashMap defines )
        throws AntException
    {
        final TaskletContext context = project.getContext();
        
        final Iterator keys = defines.keySet().iterator();
        //make sure these come before following so they get overidden if user tries to 
        //confuse the system
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final String value = (String)defines.get( key );
            context.setProperty( key, value );
        }
        
        context.setProperty( AntContextResources.HOME_DIR, m_homeDir );
        context.setProperty( AntContextResources.BIN_DIR, m_binDir );
        context.setProperty( AntContextResources.LIB_DIR, m_libDir );
        context.setProperty( AntContextResources.TASKLIB_DIR, m_taskLibDir );
        //context.put( AntContextResources.USER_DIR, m_userDir );
        context.setProperty( TaskletContext.LOGGER, m_logger );
        context.setProperty( TaskletContext.JAVA_VERSION, getJavaVersion() );
    }

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

    protected ProjectEngine getProjectEngine()
    {
        final ProjectEngine engine = createProjectEngine();
        engine.setLogger( m_logger );
        return engine;
    }

    protected ProjectEngine createProjectEngine()
    {
        return (ProjectEngine)createObject( DEFAULT_ENGINE, "project-engine" );
    }

    protected File getHomeDir( final String homeDir )
        throws AntException
    {
        final File file = new File( homeDir );
        checkDirectory( file, "ant-home" );
        return file;
    }

    protected File getBinDir( final String binDir )
        throws AntException
    {
        File file = (new File( binDir )).getAbsoluteFile();
        if( !file.isDirectory() ) file = file.getParentFile();
        checkDirectory( file, "bin-dir" );
        return file;
    }

    protected File getLibDir( final File antHome, String libDir )
        throws AntException
    {
        return resolveDirectory( antHome, libDir, "lib-dir" );
    }

    protected File getTaskLibDir( final File antHome, final String taskLibDir )
        throws AntException
    {
        return resolveDirectory( antHome, taskLibDir, "task-lib-dir" );
    }

    protected File resolveDirectory( final File antHome, final String dir, final String name )
        throws AntException
    {
        final File file = FileUtil.resolveFile( antHome, dir );
        checkDirectory( file, name );
        return file;
    }

    protected void checkDirectory( final File file, final String name )
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

    protected ProjectListener createListener( final String listenerName )
        throws AntException
    {
        try { return (ProjectListener)createObject( listenerName, "listener" ); }
        catch( final ClassCastException cce )
        {
            throw new AntException( "Aparently the listener named " + listenerName +
                                    " does not implement the ProjectListener interface",
                                    cce );
        }
    }

    protected Logger createLogger( final String logLevel )
        throws AntException
    {
        final String logLevelCapitalized = logLevel.toUpperCase();
        final Priority.Enum priority = LogKit.getPriorityForName( logLevelCapitalized );

        if( !priority.getName().equals( logLevelCapitalized ) )
        {
            throw new AntException( "Unknown log level - " + logLevel );
        }
        
        final Category category = LogKit.createCategory( "ant", priority );
        return LogKit.createLogger( category );
    }

    protected void setupDefaultAntDirs()
    {
        final String os = System.getProperty( "os.name" );
        final String userDir = System.getProperty( "user.home" );
        m_userDir = 
            (new File( getUserLocationFor( os, userDir ) )).getAbsoluteFile();
    }

    /**
     * Retrieve default bin-dir value if possible (Otherwise throw an exception).
     *
     * Lookup OS specific places for ant to be. 
     * /opt/ant on *BSD ?
     * /usr/local/ant on linux ?
     * /Program Files/Ant on Win32 ?
     *
     * @return bin directory
     */
    protected File getDefaultHomeDir()
        throws AntException
    {
        if( null != m_userDir )
        {
            try 
            {
                checkDirectory( m_userDir, null );
                return m_userDir;
            }
            catch( final AntException ae ) {}
        }

        final String os = System.getProperty( "os.name" );
        final File candidate = 
            (new File( getSystemLocationFor( os ) )).getAbsoluteFile();
        checkDirectory( candidate, "ant-home" );
        return candidate;
    }

    /**
     * This determins a mapping from an OS specific place to ants home directory.
     * In later versions the mapping should be read from configuration file.
     *
     * @param os the name of OS
     * @return the location of directory
     */
    protected String getUserLocationFor( final String os, final String userDir )
    {
        if( os.startsWith( "Windows" ) )
        {
            return userDir + "\\Ant";
        }
        else if( '/' == File.separatorChar )
        {
            if( os.startsWith( "Linux" ) ) return userDir + "/ant";
            else return userDir + "/opt/ant";
        }
        else
        {
            return userDir + File.separator + "ant";
        }
    }

    /**
     * This determins a mapping from an OS specific place to ants home directory.
     * In later versions the mapping should be read from configuration file.
     *
     * @param os the name of OS
     * @return the location of directory
     */
    protected String getSystemLocationFor( final String os )
    {
        if( os.startsWith( "Windows" ) )
        {
            return "\\Program Files\\Ant";
        }
        else if( '/' == File.separatorChar )
        {
            if( os.startsWith( "Linux" ) ) return "/usr/local/ant";
            else return "/opt/ant";
        }
        else
        {
            return File.separator + "ant";
        }
    }

    protected String getDefaultLibDir()
    {
        return DEFAULT_LIB_DIRECTORY;
    }

    protected String getDefaultTaskLibDir()
    {
        return DEFAULT_TASKLIB_DIRECTORY;
    }

    /**
     * Retrieve default filename. Overide this in base classes to change default.
     *
     * @return the default filename
     */
    protected String getDefaultFilename()
    {
        return DEFAULT_FILENAME;
    }
 
    /**
     * Retrieve default logelevel. Overide this in base classes to change default.
     *
     * @return the default loglevel
     */
    protected String getDefaultLogLevel()
    {
        return DEFAULT_LOGLEVEL;
    }
   
    /**
     * Retrieve default listener. Overide this in base classes to change default.
     *
     * @return the default listener
     */
    protected String getDefaultListener()
    {
        return DEFAULT_LISTENER;
    }

    /**
     * Get File object for filename.
     * Check that file exists and is not a directory.
     *
     * @param filename the filename
     * @return the file object
     * @exception AntException if an error occurs
     */
    protected File getFile( final String filename )
        throws AntException, IOException
    {
        final File file = (new File( filename )).getCanonicalFile();
            
        if( !file.exists() )
        {
            throw new AntException( "File " + file + " does not exist." );
        }
        
        if( file.isDirectory() )
        {
            throw new AntException( "File " + file + " is a directory." );
        }

        return file;
    }

    /**
     * Create instance of Builder based on classname.
     *
     * @param builderName builder class name
     * @return the ProjectBuilder
     * @exception AntException if an error occurs
     */
    protected ProjectBuilder createBuilder( final String builderName )
        throws AntException
    {
        try { return (ProjectBuilder)createObject( builderName, "builder" ); }
        catch( final ClassCastException cce )
        {
            throw new AntException( "Aparently the builder named " + builderName +
                                    " does not implement the ProjectBuilder interface",
                                    cce );
        }
    }

    /**
     * Helper method to create object and throw an apporpriate AntException if creation failed.
     *
     * @param objectName the classname of object
     * @param type the type of object being created (ie builder|listener)
     * @return the created object
     * @exception AntException if an error occurs
     */
    protected Object createObject( final String objectName, final String type )
        throws AntException
    {
        try
        {
            final Class clazz = Class.forName( objectName ); 
            return clazz.newInstance();
        }
        catch( final IllegalAccessException iae )
        { 
            throw new AntException( "Non-public constructor for " + type + " " + objectName, 
                                    iae );
        }
        catch( final InstantiationException ie )
        {
            throw new AntException( "Error instantiating class for " + type + " " + objectName, 
                                    ie );
        }
        catch( final ClassNotFoundException cnfe )
        {
            throw new AntException( "Could not find the class for " + type + " " + objectName, 
                                    cnfe );
        }
    }

    /**
     * Retrieve class name of builder for file.
     * Eventually this will look in a registry of file extentions to BuilderNames.
     *
     * @param filename the filename
     * @return the name of Class for Builder
     * @exception AntException if an error occurs
     */
    protected String getBuilderNameFor( final String filename )
        throws AntException
    {
        return DEFAULT_BUILDER;
    }
}

