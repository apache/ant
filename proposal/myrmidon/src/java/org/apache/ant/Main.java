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
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import org.apache.ant.launcher.AntLoader;
import org.apache.ant.project.DefaultProjectEngine;
import org.apache.ant.project.Project;
import org.apache.ant.project.ProjectBuilder;
import org.apache.ant.project.ProjectEngine;
import org.apache.ant.project.ProjectListener;
import org.apache.ant.project.LogTargetToListenerAdapter;
import org.apache.ant.tasklet.JavaVersion;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.ant.tasklet.engine.TskDeployer;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.util.ObjectUtil;
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
    //Constants to indicate the build of Ant/Myrmidon
    public final static String     BUILD_DATE                = "@@DATE@@";
    public final static String     BUILD_VERSION             = "@@VERSION@@";
    public final static String     VERSION                   = 
        "Ant " + BUILD_VERSION + " compiled on " + BUILD_DATE;

    //default log level
    protected final static String  DEFAULT_LOGLEVEL          = "WARN";

    //Some defaults for file locations/names
    protected final static String  DEFAULT_LIB_DIRECTORY     = "lib";
    protected final static String  DEFAULT_TASKLIB_DIRECTORY = DEFAULT_LIB_DIRECTORY;
    protected final static String  DEFAULT_FILENAME          = "build.xmk";

    //some constants that define the classes to be loaded to perform 
    //particular services
    protected final static String  DEFAULT_ENGINE            = 
        "org.apache.ant.project.DefaultProjectEngine";

    protected final static String  DEFAULT_LISTENER          = 
        "org.apache.ant.project.DefaultProjectListener";

    protected final static String  DEFAULT_BUILDER           =  
        "org.apache.ant.project.DefaultProjectBuilder";

    //defines for the Command Line options
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

    /**
     * Main entry point called to run standard Ant.
     *
     * @param args the args
     */
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
     * This is called by super-class.
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
                                    "Define a variable (ie -Dfoo=var)",
                                    new int[ 0 ] );
        return options;
    }

    /**
     * Entry point for standard ant.
     *
     * @param clOptions the list of command line options
     */
    protected void execute( final List clOptions )
        throws Exception
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

        //try to auto-discover the location of ant so that 
        //can populate classpath with libs/tasks and gain access
        //to antRun
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

        //setup classloader so that it will correctly load
        //the Project/ProjectBuilder/ProjectEngine and all dependencies
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

        //loop over build if we are in incremental mode..
        while( true )
        {
            //actually do the build ...
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

        //shutdown engine gracefully if needed
        if( engine instanceof Disposable )
        {
            ((Disposable)engine).dispose();
        }
    }

    /**
     * Deploy all tasklibs in tasklib directory into ProjectEngine.
     *
     * @param engine the ProjectEngine
     * @param taskLibDirectory the directory to look for .tsk files
     */
    protected void deployDefaultTaskLibs( final ProjectEngine engine, 
                                          final File taskLibDirectory )
    
    {
        final ExtensionFileFilter filter = new ExtensionFileFilter( ".tsk" );

        final File[] files = taskLibDirectory.listFiles( filter );
        final TskDeployer deployer = engine.getTaskletEngine().getTskDeployer();

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

    /**
     * Actually do the build.
     *
     * @param engine the engine
     * @param project the project
     * @param targets the targets to build as passed by CLI
     */
    protected void doBuild( final ProjectEngine engine, 
                            final Project project, 
                            final ArrayList targets )
    {
        try
        {
            final int targetCount = targets.size();
        
            //if we didn't specify a target on CLI then choose default
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
    
    /**
     * Setup Logger for a particular log-level. 
     * This is in seperate method so it can be overidden if sub-classed.
     *
     * @param logLevel the log-level
     */
    protected void setupLogger( final String logLevel )
    {
        m_logger = createLogger( logLevel );
    }

    /**
     * Create Logger of appropriate log-level.
     *
     * @param logLevel the log-level
     * @return the logger
     * @exception AntException if an error occurs
     */
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

    /**
     * Setup project listener.
     *
     * @param listenerName the name of project listener
     */
    protected void setupListener( final String listenerName )
    {
        m_listener = createListener( listenerName );
        m_logger.addLogTarget( new LogTargetToListenerAdapter( m_listener ) );
    }

    /**
     * Make sure classloader is setup correctly so can do Class.forName safely
     *
     * @param libDir the directory to grab all the lib files from
     */
    protected void setupContextClassLoader( final File libDir )
    {
        setupClassLoader( libDir );
        Thread.currentThread().setContextClassLoader( AntLoader.getLoader() );
    }

    /**
     * Setup classloader so that the *current* classloader has access to parsers etc.
     * This is a bit of a hack as it assumes that AntLoader was used to load this file
     * but it is the only way to add to current classloader safely.
     *
     * @param libDir the directory of lib files to add
     */
    protected void setupClassLoader( final File libDir )
    {
        final ExtensionFileFilter filter = 
            new ExtensionFileFilter( new String[] { ".jar", ".zip" } );

        final File[] files = libDir.listFiles( filter );

        final AntLoader classLoader = AntLoader.getLoader();

        for( int i = 0; i < files.length; i++ )
        {
            //except for a few *special* files add all the 
            //.zip/.jars to classloader
            if( !files[ i ].getName().equals( "ant.jar" ) &&
                !files[ i ].getName().equals( "myrmidon.jar" ) &&
                !files[ i ].getName().equals( "avalonapi.jar" ) )
            {                
                try { classLoader.addURL( files[ i ].toURL() ); }
                catch( final MalformedURLException mue ) {}
            }
        }        
    }

    /**
     * Using a specified builder create a project from a particular file.
     *
     * @param builderName the name of the builder class
     * @param file the file
     * @return the newly created Project
     * @exception AntException if an error occurs
     * @exception IOException if an error occurs
     */
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

    /**
     * Setup the projects context so all the "default" properties are defined.
     * This also takes a hashmap that is added to context. Usually these are the 
     * ones defined on command line.
     *
     * @param project the project
     * @param defines the defines
     * @exception AntException if an error occurs
     */
    protected void setupProjectContext( final Project project, final HashMap defines )
        throws AntException
    {
        //put these values into defines so that they overide
        //user-defined proeprties
        defines.put( AntContextResources.HOME_DIR, m_homeDir );
        defines.put( AntContextResources.BIN_DIR, m_binDir );
        defines.put( AntContextResources.LIB_DIR, m_libDir );
        defines.put( AntContextResources.TASKLIB_DIR, m_taskLibDir );
        //defines.put( AntContextResources.USER_DIR, m_userDir );
        defines.put( TaskletContext.JAVA_VERSION, getJavaVersion() );

        final TaskletContext context = project.getContext();
        addToContext( context, defines );

        //Add system properties second so that they overide user-defined properties
        addToContext( context, System.getProperties() );
    }

    /**
     * Helper method to add values to a context
     *
     * @param context the context
     * @param map the map of names->values
     */
    protected void addToContext( final TaskletContext context, final Map map )
    {
        final Iterator keys = map.keySet().iterator();

        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final Object value = map.get( key );
            context.setProperty( key, value );
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
     * Create and configure project engine
     *
     * @return the ProjectEngine
     */
    protected ProjectEngine getProjectEngine()
    {
        final ProjectEngine engine = createProjectEngine();
        engine.setLogger( m_logger );
        return engine;
    }

    /**
     * Create the project engine.
     * This is seperate method so that it can be overidden in a sub-class.
     *
     * @return the new ProjectEngine
     */
    protected ProjectEngine createProjectEngine()
    {
        return (ProjectEngine)createObject( DEFAULT_ENGINE, "project-engine" );
    }

    protected File getHomeDir( final String homeDir )
        throws AntException
    {
        final File file = (new File( homeDir )).getAbsoluteFile();
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
            return ObjectUtil.createObject( objectName ); 
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

