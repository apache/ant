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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.ant.launcher.AntClassLoader;
import org.apache.ant.launcher.AntLoader;
import org.apache.ant.project.LogTargetToListenerAdapter;
import org.apache.ant.project.Project;
import org.apache.ant.project.ProjectBuilder;
import org.apache.ant.project.ProjectEngine;
import org.apache.ant.project.ProjectListener;
import org.apache.ant.tasklet.JavaVersion;
import org.apache.ant.tasklet.TaskletContext;
import org.apache.ant.tasklet.engine.TaskletEngine;
import org.apache.avalon.Disposable;
import org.apache.avalon.Initializable;
import org.apache.avalon.camelot.CamelotUtil;
import org.apache.avalon.camelot.Deployer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.util.ObjectUtil;
import org.apache.avalon.util.StringUtil;
import org.apache.avalon.util.cli.AbstractMain;
import org.apache.avalon.util.cli.CLOption;
import org.apache.avalon.util.cli.CLOptionDescriptor;
import org.apache.avalon.util.io.ExtensionFileFilter;
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
    public final static String     VERSION                   = 
        "Ant " + Constants.BUILD_VERSION + " compiled on " + Constants.BUILD_DATE;

    //default log level
    protected final static String  DEFAULT_LOGLEVEL          = "WARN";

    //Some defaults for file locations/names
    protected final static String  DEFAULT_FILENAME          = "build.xmk";

    protected final static String  DEFAULT_LISTENER          = 
        "org.apache.ant.project.DefaultProjectListener";

    //defines for the Command Line options
    private static final int       HELP_OPT                  = 'h';
    private static final int       QUIET_OPT                 = 'q';
    private static final int       VERBOSE_OPT               = 'v';
    private static final int       FILE_OPT                  = 'f';
    private static final int       LOG_LEVEL_OPT             = 'l';
    private static final int       DEFINE_OPT                = 'D';
    private static final int       VERSION_OPT               = 1;
    private static final int       LISTENER_OPT              = 2;
    private static final int       TASKLIB_DIR_OPT           = 5;
    private static final int       INCREMENTAL_OPT           = 6;
    private static final int       HOME_DIR_OPT              = 7;
    
    //incompatable options for info options
    private static final int[]     INFO_OPT_INCOMPAT         = new int[] 
    { 
        HELP_OPT, QUIET_OPT, VERBOSE_OPT, FILE_OPT, 
        LOG_LEVEL_OPT, VERSION_OPT, LISTENER_OPT,
        DEFINE_OPT //TASKLIB_DIR_OPT, HOME_DIR_OPT
    };
    
    //incompatable options for other logging options
    private static final int[]     LOG_OPT_INCOMPAT          = new int[] 
    {
        QUIET_OPT, VERBOSE_OPT, LOG_LEVEL_OPT
    };

    protected ProjectListener      m_listener;
    protected File                 m_homeDir;

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
            main.getLogger().error( "Error: " + ae.getMessage() );
            main.getLogger().debug( "Exception..." + StringUtil.printStackTrace( ae ) );
        }
        catch( final Throwable throwable )
        {
            main.getLogger().error( "Error: " + throwable );
            main.getLogger().debug( "Exception..." + StringUtil.printStackTrace( throwable ) );
        }
    }

    /**
     * Initialise the options for command line parser.
     * This is called by super-class.
     */
    protected CLOptionDescriptor[] createCLOptions()
    {
        //TODO: localise
        final CLOptionDescriptor[] options = new CLOptionDescriptor[ 11 ];

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
            new CLOptionDescriptor( "task-lib-dir",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    TASKLIB_DIR_OPT,
                                    "the task lib directory to scan for .tsk files." );
        options[8] =
            new CLOptionDescriptor( "incremental",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    INCREMENTAL_OPT,
                                    "Run in incremental mode" );
        options[9] =
            new CLOptionDescriptor( "ant-home",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    HOME_DIR_OPT,
                                    "Specify ant home directory" );
        options[10] =
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
        String logLevel = null;
        String homeDir = null;
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

        if( null == logLevel ) logLevel = DEFAULT_LOGLEVEL;
        if( null == listenerName ) listenerName = DEFAULT_LISTENER;
        if( null == filename ) filename = DEFAULT_FILENAME;

        //handle logging...
        setLogger( createLogger( logLevel ) );

        //if ant home not set then use system property ant.home 
        //that was set up by launcher.
        if( null == homeDir ) homeDir = System.getProperty( "ant.home" );

        final Properties properties = new Properties();
        properties.setProperty( "ant.home", homeDir );

        if( null != taskLibDir ) properties.setProperty( "ant.path.task-lib", taskLibDir );

        m_homeDir = (new File( homeDir )).getAbsoluteFile();
        if( !m_homeDir.isDirectory() )
        {
            throw new AntException( "ant-home (" + m_homeDir + ") is not a directory" );
        }

        final File libDir = new File( m_homeDir, "lib" );

        final File buildFile = (new File( filename )).getCanonicalFile();
        if( !buildFile.isFile() )
        {
            throw new AntException( "File " + buildFile + " is not a file or doesn't exist" );
        }
        
        //setup classloader so that it will correctly load
        //the Project/ProjectBuilder/ProjectEngine and all dependencies
        final ClassLoader classLoader = createClassLoader( libDir );
        Thread.currentThread().setContextClassLoader( classLoader );

        //handle listener.. 
        final ProjectListener listener = createListener( listenerName ); 

        getLogger().warn( "Ant Build File: " + buildFile );
        getLogger().info( "Ant Home Directory: " + m_homeDir );
        //getLogger().info( "Ant Bin Directory: " + m_binDir );
        //getLogger().debug( "Ant Lib Directory: " + m_libDir );
        //getLogger().debug( "Ant Task Lib Directory: " + m_taskLibDir );

        final AntEngine antEngine = new DefaultAntEngine();
        setupLogger( antEngine );
        antEngine.setProperties( properties );
        antEngine.init();

        final ProjectBuilder builder = antEngine.getProjectBuilder();

        //create the project
        final Project project = builder.build( buildFile );
        setupProjectContext( project, defines );

        final ProjectEngine engine = antEngine.getProjectEngine();
        engine.addProjectListener( listener );

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

        antEngine.dispose();
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
            getLogger().error( "BUILD FAILED" );
            getLogger().error( "Reason:\n" + StringUtil.printStackTrace( ae, 5, true ) );
        }
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
    protected ProjectListener createListener( final String listenerName )
        throws AntException
    {
        ProjectListener result = null;

        try { result = (ProjectListener)ObjectUtil.createObject( listenerName ); }
        catch( final Throwable t )
        {
            throw new AntException( "Error creating the listener " + listenerName + 
                                    " due to " + StringUtil.printStackTrace( t, 5, true ), 
                                    t );
        }

        getLogger().addLogTarget( new LogTargetToListenerAdapter( result ) );

        return result;
    }

    /**
     * Try to load all extra zipz/jars from lib directory into CURRENT classloader.
     *
     * @param libDir the directory of lib files to add
     */
    protected ClassLoader createClassLoader( final File libDir )
    {
        final ClassLoader candidate = getClass().getClassLoader();
        
        if( !(candidate instanceof AntClassLoader) )
        {
            getLogger().warn( "Warning: Unable to add entries from " + 
                              "lib-path to classloader" );
            return candidate;
        }
        
        final AntClassLoader classLoader = (AntClassLoader)candidate;

        final ExtensionFileFilter filter = 
            new ExtensionFileFilter( new String[] { ".jar", ".zip" } );

        final File[] files = libDir.listFiles( filter );

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

        return classLoader;
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
        //defines.put( AntContextResources.HOME_DIR, m_homeDir );
        //defines.put( AntContextResources.BIN_DIR, m_binDir );
        //defines.put( AntContextResources.LIB_DIR, m_libDir );
        //defines.put( AntContextResources.TASKLIB_DIR, m_taskLibDir );
        //defines.put( TaskletContext.JAVA_VERSION, getJavaVersion() );

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
}

