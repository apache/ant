/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.frontends;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.avalon.excalibur.cli.CLArgsParser;
import org.apache.avalon.excalibur.cli.CLOption;
import org.apache.avalon.excalibur.cli.CLOptionDescriptor;
import org.apache.avalon.excalibur.cli.CLUtil;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Hierarchy;
import org.apache.log.LogTarget;
import org.apache.log.Logger;
import org.apache.log.Priority;
import org.apache.log.output.DefaultOutputLogTarget;
import org.apache.myrmidon.Constants;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.embeddor.DefaultEmbeddor;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.executor.Executor;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * The class to kick the tires and light the fires.
 * Starts myrmidon, loads ProjectBuilder, builds project then uses ProjectManager
 * to run project.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class CLIMain
    extends AbstractLogEnabled
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( CLIMain.class );

    //defines for the Command Line options
    private static final int HELP_OPT = 'h';
    private static final int QUIET_OPT = 'q';
    private static final int VERBOSE_OPT = 'v';
    private static final int FILE_OPT = 'f';
    private static final int LOG_LEVEL_OPT = 'l';
    private static final int DEFINE_OPT = 'D';
    private static final int BUILDER_PARAM_OPT = 'B';
    private static final int VERSION_OPT = 1;
    private static final int LISTENER_OPT = 2;
    private static final int TASKLIB_DIR_OPT = 5;
    private static final int INCREMENTAL_OPT = 6;
    private static final int HOME_DIR_OPT = 7;
    private static final int DRY_RUN_OPT = 8;

    //incompatable options for info options
    private static final int[] INFO_OPT_INCOMPAT = new int[]
    {
        HELP_OPT, QUIET_OPT, VERBOSE_OPT, FILE_OPT,
        LOG_LEVEL_OPT, VERSION_OPT, LISTENER_OPT,
        DEFINE_OPT, DRY_RUN_OPT //TASKLIB_DIR_OPT, HOME_DIR_OPT
    };

    //incompatable options for other logging options
    private static final int[] LOG_OPT_INCOMPAT = new int[]
    {
        QUIET_OPT, VERBOSE_OPT, LOG_LEVEL_OPT
    };

    private ProjectListener m_listener;

    ///Parameters for run of myrmidon
    private Parameters m_parameters = new Parameters();

    ///List of targets supplied on command line to execute
    private ArrayList m_targets = new ArrayList();

    ///List of user supplied defines
    private Parameters m_defines = new Parameters();

    ///List of user supplied parameters for builder
    private Parameters m_builderParameters = new Parameters();

    ///Determine whether tasks are actually executed
    private boolean m_dryRun = false;

    /**
     * Main entry point called to run standard Myrmidon.
     *
     * @param args the args
     */
    public static void main( final String[] args )
    {
        final CLIMain main = new CLIMain();

        try
        {
            main.execute( args );
        }
        catch( final Throwable throwable )
        {
            final String message =
                REZ.getString( "error-message", ExceptionUtil.printStackTrace( throwable ) );
            System.err.println( message );
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
        System.out.println( "\tAvailable options:" );
        System.out.println( CLUtil.describeOptions( options ) );
    }

    /**
     * Initialise the options for command line parser.
     */
    private CLOptionDescriptor[] createCLOptions()
    {
        //TODO: localise
        final CLOptionDescriptor[] options = new CLOptionDescriptor[ 13 ];
        options[ 0 ] =
            new CLOptionDescriptor( "help",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    HELP_OPT,
                                    REZ.getString( "help.opt" ),
                                    INFO_OPT_INCOMPAT );
        options[ 1 ] =
            new CLOptionDescriptor( "file",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    FILE_OPT,
                                    REZ.getString( "file.opt" ) );
        options[ 2 ] =
            new CLOptionDescriptor( "log-level",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LOG_LEVEL_OPT,
                                    REZ.getString( "log-level.opt" ),
                                    LOG_OPT_INCOMPAT );
        options[ 3 ] =
            new CLOptionDescriptor( "quiet",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    QUIET_OPT,
                                    REZ.getString( "quiet.opt" ),
                                    LOG_OPT_INCOMPAT );
        options[ 4 ] =
            new CLOptionDescriptor( "verbose",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    VERBOSE_OPT,
                                    REZ.getString( "verbose.opt" ),
                                    LOG_OPT_INCOMPAT );
        options[ 5 ] =
            new CLOptionDescriptor( "listener",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LISTENER_OPT,
                                    REZ.getString( "listener.opt" ) );
        options[ 6 ] =
            new CLOptionDescriptor( "version",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    VERSION_OPT,
                                    REZ.getString( "version.opt" ),
                                    INFO_OPT_INCOMPAT );

        options[ 7 ] =
            new CLOptionDescriptor( "task-lib-dir",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    TASKLIB_DIR_OPT,
                                    REZ.getString( "tasklib.opt" ) );
        options[ 8 ] =
            new CLOptionDescriptor( "incremental",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    INCREMENTAL_OPT,
                                    REZ.getString( "incremental.opt" ) );
        options[ 9 ] =
            new CLOptionDescriptor( "myrmidon-home",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    HOME_DIR_OPT,
                                    REZ.getString( "home.opt" ) );
        options[ 10 ] =
            new CLOptionDescriptor( "define",
                                    CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                                    DEFINE_OPT,
                                    REZ.getString( "define.opt" ),
                                    new int[ 0 ] );
        options[ 11 ] =
            new CLOptionDescriptor( "builder-parameter",
                                    CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                                    BUILDER_PARAM_OPT,
                                    REZ.getString( "build.opt" ) );
        options[ 12 ] =
            new CLOptionDescriptor( "dry-run",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    DRY_RUN_OPT,
                                    REZ.getString( "dry-run.opt" ) );
        return options;
    }

    private boolean parseCommandLineOptions( final String[] args )
    {
        final CLOptionDescriptor[] options = createCLOptions();
        final CLArgsParser parser = new CLArgsParser( args, options );

        if( null != parser.getErrorString() )
        {
            final String message = REZ.getString( "error-message", parser.getErrorString() );
            System.err.println( message );
            return false;
        }

        final List clOptions = parser.getArguments();
        final int size = clOptions.size();

        for( int i = 0; i < size; i++ )
        {
            final CLOption option = (CLOption)clOptions.get( i );

            switch( option.getId() )
            {
                case HELP_OPT:
                    usage( options );
                    return false;
                case VERSION_OPT:
                    System.out.println( Constants.BUILD_DESCRIPTION );
                    return false;

                case HOME_DIR_OPT:
                    m_parameters.setParameter( "myrmidon.home", option.getArgument() );
                    break;
                case TASKLIB_DIR_OPT:
                    m_parameters.setParameter( "myrmidon.lib.path", option.getArgument() );
                    break;

                case LOG_LEVEL_OPT:
                    m_parameters.setParameter( "log.level", option.getArgument() );
                    break;
                case VERBOSE_OPT:
                    m_parameters.setParameter( "log.level", "INFO" );
                    break;
                case QUIET_OPT:
                    m_parameters.setParameter( "log.level", "ERROR" );
                    break;

                case INCREMENTAL_OPT:
                    m_parameters.setParameter( "incremental", "true" );
                    break;

                case FILE_OPT:
                    m_parameters.setParameter( "filename", option.getArgument() );
                    break;
                case LISTENER_OPT:
                    m_parameters.setParameter( "listener", option.getArgument() );
                    break;

                case DEFINE_OPT:
                    m_defines.setParameter( option.getArgument( 0 ), option.getArgument( 1 ) );
                    break;

                case BUILDER_PARAM_OPT:
                    m_builderParameters.setParameter( option.getArgument( 0 ), option.getArgument( 1 ) );
                    break;

                case DRY_RUN_OPT:
                    m_dryRun = true;
                    break;

                case 0:
                    m_targets.add( option.getArgument() );
                    break;
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
        enableLogging( new LogKitLogger( createLogger( logLevel ) ) );

        final String home = m_parameters.getParameter( "myrmidon.home", null );
        final File homeDir = ( new File( home ) ).getAbsoluteFile();
        if( !homeDir.isDirectory() )
        {
            final String message = REZ.getString( "home-not-dir.error", homeDir );
            throw new Exception( message );
        }

        final String filename = m_parameters.getParameter( "filename", null );
        final File buildFile = ( new File( filename ) ).getCanonicalFile();
        if( !buildFile.isFile() )
        {
            final String message = REZ.getString( "bad-file.error", buildFile );
            throw new Exception( message );
        }

        //handle listener..
        final String listenerName = m_parameters.getParameter( "listener", null );
        final ProjectListener listener = createListener( listenerName );

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "buildfile.notice", buildFile );
            getLogger().warn( message );
        }

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "homedir.notice", homeDir );
            getLogger().info( message );
        }
        //getLogger().info( "Ant Bin Directory: " + m_binDir );
        //getLogger().debug( "Ant Lib Directory: " + m_libDir );
        //getLogger().debug( "Ant Task Lib Directory: " + m_taskLibDir );

        if( m_dryRun )
        {
            m_parameters.setParameter( Executor.ROLE,
                                       "org.apache.myrmidon.components.executor.PrintingExecutor" );
        }

        final Embeddor embeddor = new DefaultEmbeddor();
        setupLogger( embeddor );
        embeddor.parameterize( m_parameters );
        embeddor.initialize();
        embeddor.start();

        //create the project
        final Project project =
            embeddor.createProject( buildFile.toString(), null, m_builderParameters );

        BufferedReader reader = null;

        //loop over build if we are in incremental mode..
        final boolean incremental = m_parameters.getParameterAsBoolean( "incremental", false );
        while( true )
        {
            //actually do the build ...
            final Workspace workspace = embeddor.createWorkspace( m_defines );
            workspace.addProjectListener( listener );

            doBuild( workspace, project, m_targets );

            if( !incremental ) break;

            final String message = REZ.getString( "repeat.notice" );
            System.out.println( message );

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
    private void doBuild( final Workspace workspace,
                          final Project project,
                          final ArrayList targets )
    {
        try
        {
            final int targetCount = targets.size();

            //if we didn't specify a target on CLI then choose default
            if( 0 == targetCount )
            {
                workspace.executeProject( project, project.getDefaultTargetName() );
            }
            else
            {
                for( int i = 0; i < targetCount; i++ )
                {
                    workspace.executeProject( project, (String)targets.get( i ) );
                }
            }
        }
        catch( final TaskException ae )
        {
            final String message =
                REZ.getString( "build-failed.error", ExceptionUtil.printStackTrace( ae, 5, true ) );
            getLogger().error( message );
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
            final String message = REZ.getString( "bad-loglevel.error", logLevel );
            throw new Exception( message );
        }

        final Logger logger = Hierarchy.getDefaultHierarchy().getLoggerFor( "myrmidon" );

        final DefaultOutputLogTarget target = new DefaultOutputLogTarget();
        target.setFormat( "[%8.8{category}] %{message}\\n%{throwable}" );
        logger.setLogTargets( new LogTarget[]{target} );

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
        try
        {
            return (ProjectListener)Class.forName( listener ).newInstance();
        }
        catch( final Throwable t )
        {
            final String message =
                REZ.getString( "bad-listener.error",
                               listener,
                               ExceptionUtil.printStackTrace( t, 5, true ) );
            throw new Exception( message );
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

