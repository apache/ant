/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon;

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
import org.apache.avalon.excalibur.cli.CLArgsParser;
import org.apache.avalon.excalibur.cli.CLOption;
import org.apache.avalon.excalibur.cli.CLOptionDescriptor;
import org.apache.avalon.excalibur.cli.CLUtil;
import org.apache.avalon.excalibur.io.ExtensionFileFilter;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Hierarchy;
import org.apache.log.LogTarget;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.apache.myrmidon.api.DefaultTaskContext;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.builder.ProjectBuilder;
import org.apache.myrmidon.components.embeddor.Embeddor;
import org.apache.myrmidon.components.embeddor.MyrmidonEmbeddor;
import org.apache.myrmidon.components.manager.LogTargetToListenerAdapter;
import org.apache.myrmidon.components.manager.ProjectManager;
import org.apache.myrmidon.components.model.Project;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * The class to kick the tires and light the fires.
 * Starts myrmidon, loads ProjectBuilder, builds project then uses ProjectManager
 * to run project.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Main
    extends AbstractLoggable
{
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

    private ProjectListener      m_listener;

    ///Parameters for run of myrmidon
    private Parameters           m_parameters  = new Parameters();

    ///List of targets supplied on command line to execute
    private ArrayList            m_targets     = new ArrayList();

    ///List of user supplied defines
    private HashMap              m_defines     = new HashMap();

    /**
     * Main entry point called to run standard Myrmidon.
     *
     * @param args the args
     */
    public static void main( final String[] args )
    {
        final Main main = new Main();

        try { main.execute( args ); }
        catch( final Throwable throwable )
        {
            System.err.println( "Error: " + ExceptionUtil.printStackTrace( throwable ) );
            System.exit( -1 );
        }

        System.exit( 0 );
    }

    /**
     * Display usage report.
     *
     */
    private void usage( final CLOptionDescriptor[] options )
    {
        System.out.println( "java " + getClass().getName() + " [options]" );
        System.out.println( "\tAvailable options:");
        System.out.println( CLUtil.describeOptions( options ) );
    }

    /**
     * Initialise the options for command line parser.
     */
    private CLOptionDescriptor[] createCLOptions()
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
            new CLOptionDescriptor( "myrmidon-home",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    HOME_DIR_OPT,
                                    "Specify myrmidon home directory" );
        options[10] =
            new CLOptionDescriptor( "define",
                                    CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                                    DEFINE_OPT,
                                    "Define a variable (ie -Dfoo=var)",
                                    new int[ 0 ] );
        return options;
    }

    private boolean parseCommandLineOptions( final String[] args )
    {
        final CLOptionDescriptor[] options = createCLOptions();
        final CLArgsParser parser = new CLArgsParser( args, options );

        if( null != parser.getErrorString() )
        {
            System.err.println( "Error: " + parser.getErrorString() );
            return false;
        }

        final List clOptions = parser.getArguments();
        final int size = clOptions.size();

        for( int i = 0; i < size; i++ )
        {
            final CLOption option = (CLOption)clOptions.get( i );

            switch( option.getId() )
            {
            case HELP_OPT: usage( options ); return false;
            case VERSION_OPT: System.out.println( Constants.BUILD_DESCRIPTION ); return false;

            case HOME_DIR_OPT: m_parameters.setParameter( "myrmidon.home", option.getArgument() ); break;
            case TASKLIB_DIR_OPT: 
                m_parameters.setParameter( "myrmidon.lib.path", option.getArgument() ); 
                break;

            case LOG_LEVEL_OPT: m_parameters.setParameter( "log.level", option.getArgument() ); break;
            case VERBOSE_OPT: m_parameters.setParameter( "log.level", "INFO" ); break;
            case QUIET_OPT: m_parameters.setParameter( "log.level", "ERROR" ); break;

            case INCREMENTAL_OPT: m_parameters.setParameter( "incremental", "true" ); break;

            case FILE_OPT: m_parameters.setParameter( "filename", option.getArgument() ); break;
            case LISTENER_OPT: m_parameters.setParameter( "listener", option.getArgument() ); break;

            case DEFINE_OPT:
                m_defines.put( option.getArgument( 0 ), option.getArgument( 1 ) );
                break;

            case 0: m_targets.add( option.getArgument() ); break;
            }
        }

        return true;
    }

    private void setupDefaultParameters()
    {
        //System property set up by launcher
        m_parameters.setParameter( "myrmidon.home", System.getProperty( "myrmidon.home", "." ) );

        m_parameters.setParameter( "filename", "build.ant" );
        m_parameters.setParameter( "log.level", "WARN" );
        m_parameters.setParameter( "listener", "org.apache.myrmidon.listeners.DefaultProjectListener" );
        m_parameters.setParameter( "incremental", "false" );
    }

    private void execute( final String[] args )
        throws Exception
    {
        setupDefaultParameters();

        if( !parseCommandLineOptions( args ) )
        {
            return;
        }

        //handle logging...
        final String logLevel = m_parameters.getParameter( "log.level", null );
        setLogger( createLogger( logLevel ) );

        final String home = m_parameters.getParameter( "myrmidon.home", null );
        final File homeDir = (new File( home )).getAbsoluteFile();
        if( !homeDir.isDirectory() )
        {
            throw new Exception( "myrmidon-home (" + homeDir + ") is not a directory" );
        }

        final String filename = m_parameters.getParameter( "filename", null );
        final File buildFile = (new File( filename )).getCanonicalFile();
        if( !buildFile.isFile() )
        {
            throw new Exception( "File " + buildFile + " is not a file or doesn't exist" );
        }

        //handle listener..
        final String listenerName = m_parameters.getParameter( "listener", null );
        final ProjectListener listener = createListener( listenerName );

        final LogTarget target = new LogTargetToListenerAdapter( listener );
        getLogger().setLogTargets( new LogTarget[] { target } );

        getLogger().warn( "Ant Build File: " + buildFile );
        getLogger().info( "Ant Home Directory: " + homeDir );
        //getLogger().info( "Ant Bin Directory: " + m_binDir );
        //getLogger().debug( "Ant Lib Directory: " + m_libDir );
        //getLogger().debug( "Ant Task Lib Directory: " + m_taskLibDir );

        final Embeddor embeddor = new MyrmidonEmbeddor();
        setupLogger( embeddor );
        embeddor.parameterize( m_parameters );
        embeddor.initialize();
        embeddor.start();

        final ProjectBuilder builder = embeddor.getProjectBuilder();

        //create the project
        final Project project = builder.build( buildFile );

        final ProjectManager manager = embeddor.getProjectManager();
        manager.addProjectListener( listener );

        BufferedReader reader = null;

        //loop over build if we are in incremental mode..
        final boolean incremental = m_parameters.getParameterAsBoolean( "incremental", false );
        while( true )
        {
            //actually do the build ...
            final TaskContext context = new DefaultTaskContext();
            
            //Add CLI m_defines
            addToContext( context, m_defines );

            //Add system properties second so that they overide user-defined properties
            addToContext( context, System.getProperties() );

            context.setProperty( TaskContext.BASE_DIRECTORY, project.getBaseDirectory() );
            context.setProperty( Project.PROJECT_FILE, buildFile );
            //context.setProperty( Project.PROJECT, project.getName() );

            doBuild( manager, project, context, m_targets );

            if( !incremental ) break;

            System.out.println( "Continue ? (Enter no to stop)" );

            if( null == reader )
            {
                reader = new BufferedReader( new InputStreamReader( System.in ) );
            }

            String line = reader.readLine();

            if( line.equalsIgnoreCase( "no" ) ) break;

        }

        embeddor.stop();
        embeddor.dispose();
    }

    /**
     * Actually do the build.
     *
     * @param manager the manager
     * @param project the project
     * @param targets the targets to build as passed by CLI
     */
    private void doBuild( final ProjectManager manager,
                          final Project project,
                          final TaskContext context,
                          final ArrayList targets )
    {
        try
        {
            final int targetCount = targets.size();

            //if we didn't specify a target on CLI then choose default
            if( 0 == targetCount )
            {
                manager.executeTarget( project, project.getDefaultTargetName(), context );
            }
            else
            {
                for( int i = 0; i < targetCount; i++ )
                {
                    manager.executeTarget( project, (String)targets.get( i ), context );
                }
            }
        }
        catch( final TaskException ae )
        {
            getLogger().error( "BUILD FAILED" );
            getLogger().error( "Reason:\n" + ExceptionUtil.printStackTrace( ae, 5, true ) );
        }
    }

    /**
     * Create Logger of appropriate log-level.
     *
     * @param logLevel the log-level
     * @return the logger
     * @exception Exception if an error occurs
     */
    private Logger createLogger( final String logLevel )
        throws Exception
    {
        final String logLevelCapitalized = logLevel.toUpperCase();
        final Priority priority = Priority.getPriorityForName( logLevelCapitalized );

        if( !priority.getName().equals( logLevelCapitalized ) )
        {
            throw new Exception( "Unknown log level - " + logLevel );
        }

        final Logger logger = Hierarchy.getDefaultHierarchy().getLoggerFor( "myrmidon" );

        logger.setPriority( priority );

        return logger;
    }

    /**
     * Setup project listener.
     *
     * @param listener the classname of project listener
     */
    private ProjectListener createListener( final String listener )
        throws Exception
    {
        try { return (ProjectListener)Class.forName( listener ).newInstance(); }
        catch( final Throwable t )
        {
            throw new Exception( "Error creating the listener " + listener +
                                 " due to " + ExceptionUtil.printStackTrace( t, 5, true ) );
        }
    }

    /**
     * Helper method to add values to a context
     *
     * @param context the context
     * @param map the map of names->values
     */
    private void addToContext( final TaskContext context, final Map map )
        throws Exception
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

