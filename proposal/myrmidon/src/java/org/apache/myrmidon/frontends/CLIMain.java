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
import java.util.List;
import org.apache.avalon.excalibur.cli.CLArgsParser;
import org.apache.avalon.excalibur.cli.CLOption;
import org.apache.avalon.excalibur.cli.CLOptionDescriptor;
import org.apache.avalon.excalibur.cli.CLUtil;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.CascadingException;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.Constants;
import org.apache.myrmidon.api.TaskException;
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
 * @version $Revision$ $Date$
 */
public class CLIMain
    extends AbstractLogEnabled
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( CLIMain.class );

    private final String DEFAULT_EMBEDDOR_CLASS = "org.apache.myrmidon.components.embeddor.DefaultEmbeddor";

    //defines for the Command Line options
    private final static int HELP_OPT = 'h';
    private final static int QUIET_OPT = 'q';
    private final static int VERBOSE_OPT = 'v';
    private final static int FILE_OPT = 'f';
    private final static int LOG_LEVEL_OPT = 'l';
    private final static int DEFINE_OPT = 'D';
    private final static int BUILDER_PARAM_OPT = 'B';
    private final static int NO_PREFIX_OPT = 'p';
    private final static int VERSION_OPT = 1;
    private final static int LISTENER_OPT = 2;
    private final static int TASKLIB_DIR_OPT = 5;
    private final static int INCREMENTAL_OPT = 6;
    private final static int HOME_DIR_OPT = 7;
    private final static int DRY_RUN_OPT = 8;

    //incompatable options for info options
    private final static int[] INFO_OPT_INCOMPAT = new int[]
    {
        HELP_OPT, QUIET_OPT, VERBOSE_OPT, FILE_OPT,
        LOG_LEVEL_OPT, BUILDER_PARAM_OPT, NO_PREFIX_OPT,
        VERSION_OPT, LISTENER_OPT, TASKLIB_DIR_OPT,
        INCREMENTAL_OPT, HOME_DIR_OPT, DRY_RUN_OPT
    };

    //incompatable options for other logging options
    private final static int[] LOG_OPT_INCOMPAT = new int[]
    {
        QUIET_OPT, VERBOSE_OPT, LOG_LEVEL_OPT
    };

    //incompatible options for listener options
    private final static int[] LISTENER_OPT_INCOMPAT = new int[]
    {
        LISTENER_OPT, NO_PREFIX_OPT
    };

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

    ///Log level to use
    //private static Priority m_priority = Priority.WARN;
    private static int m_priority = BasicLogger.LEVEL_WARN;

    /**
     * Main entry point called to run standard Myrmidon.
     *
     * @param args the args
     */
    public static void main( final String[] args )
    {
        int exitCode = 0;
        final CLIMain main = new CLIMain();
        try
        {
            main.execute( args );
        }
        catch( final Throwable throwable )
        {
            main.reportError( throwable );
            exitCode = -1;
        }
        finally
        {
            System.exit( exitCode );
        }
    }

    /**
     * Display usage report.
     *
     */
    private void usage( final CLOptionDescriptor[] options )
    {
        System.out.println( "ant [options] [targets]" );
        System.out.println( "\tAvailable options:" );
        System.out.println( CLUtil.describeOptions( options ) );
    }

    /**
     * Initialise the options for command line parser.
     */
    private CLOptionDescriptor[] createCLOptions()
    {
        //TODO: localise
        final CLOptionDescriptor[] options = {
            new CLOptionDescriptor( "help",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    HELP_OPT,
                                    REZ.getString( "help.opt" ),
                                    INFO_OPT_INCOMPAT ),
            new CLOptionDescriptor( "file",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    FILE_OPT,
                                    REZ.getString( "file.opt" ) ),
            new CLOptionDescriptor( "log-level",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LOG_LEVEL_OPT,
                                    REZ.getString( "log-level.opt" ),
                                    LOG_OPT_INCOMPAT ),
            new CLOptionDescriptor( "quiet",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    QUIET_OPT,
                                    REZ.getString( "quiet.opt" ),
                                    LOG_OPT_INCOMPAT ),
            new CLOptionDescriptor( "verbose",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    VERBOSE_OPT,
                                    REZ.getString( "verbose.opt" ),
                                    LOG_OPT_INCOMPAT ),
            new CLOptionDescriptor( "listener",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    LISTENER_OPT,
                                    REZ.getString( "listener.opt" ),
                                    LISTENER_OPT_INCOMPAT ),
            new CLOptionDescriptor( "noprefix",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    NO_PREFIX_OPT,
                                    REZ.getString( "noprefix.opt" ),
                                    LISTENER_OPT_INCOMPAT ),
            new CLOptionDescriptor( "version",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    VERSION_OPT,
                                    REZ.getString( "version.opt" ),
                                    INFO_OPT_INCOMPAT ),
            new CLOptionDescriptor( "task-lib-dir",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    TASKLIB_DIR_OPT,
                                    REZ.getString( "tasklib.opt" ) ),
            new CLOptionDescriptor( "incremental",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    INCREMENTAL_OPT,
                                    REZ.getString( "incremental.opt" ) ),
            new CLOptionDescriptor( "ant-home",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    HOME_DIR_OPT,
                                    REZ.getString( "home.opt" ) ),
            new CLOptionDescriptor( "define",
                                    CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                                    DEFINE_OPT,
                                    REZ.getString( "define.opt" ),
                                    new int[ 0 ] ),
            new CLOptionDescriptor( "builder-parameter",
                                    CLOptionDescriptor.ARGUMENTS_REQUIRED_2,
                                    BUILDER_PARAM_OPT,
                                    REZ.getString( "build.opt" ) ),
            new CLOptionDescriptor( "dry-run",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    DRY_RUN_OPT,
                                    REZ.getString( "dry-run.opt" ) )
        };

        return options;
    }

    private boolean parseCommandLineOptions( final String[] args )
        throws Exception
    {
        final CLOptionDescriptor[] options = createCLOptions();
        final CLArgsParser parser = new CLArgsParser( args, options );

        if( null != parser.getErrorString() )
        {
            final String message = REZ.getString( "error-message", parser.getErrorString() );
            throw new Exception( message );
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
                    m_priority = mapLogLevel( option.getArgument() );
                    break;
                case VERBOSE_OPT:
                    m_priority = BasicLogger.LEVEL_INFO;
                    break;
                case QUIET_OPT:
                    m_priority = BasicLogger.LEVEL_ERROR;
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
                case NO_PREFIX_OPT:
                    m_parameters.setParameter( "listener", "noprefix" );
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
        m_parameters.setParameter( "listener", "default" );
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

        prepareLogging();

        checkHomeDir();
        final File buildFile = getBuildFile();

        //getLogger().debug( "Ant Bin Directory: " + m_binDir );
        //getLogger().debug( "Ant Lib Directory: " + m_libDir );
        //getLogger().debug( "Ant Task Lib Directory: " + m_taskLibDir );

        if( m_dryRun )
        {
            m_parameters.setParameter( Executor.ROLE,
                                       "org.apache.myrmidon.components.executor.PrintingExecutor" );
        }

        final Embeddor embeddor = prepareEmbeddor();

        try
        {
            final ProjectListener listener = prepareListener( embeddor );

            //create the project
            final Project project =
                embeddor.createProject( buildFile.toString(), null, m_builderParameters );

            //loop over build if we are in incremental mode..
            final boolean incremental = m_parameters.getParameterAsBoolean( "incremental", false );
            if( !incremental )
            {
                executeBuild( embeddor, project, listener );
            }
            else
            {
                executeIncrementalBuild( embeddor, project, listener );
            }
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "build-failed.error" );
            throw new CascadingException( message, e );
        }
        finally
        {
            shutdownEmbeddor( embeddor );
        }
    }

    private void executeIncrementalBuild( final Embeddor embeddor,
                                          final Project project,
                                          final ProjectListener listener )
        throws Exception
    {
        BufferedReader reader = null;

        while( true )
        {
            try
            {
                executeBuild( embeddor, project, listener );
            }
            catch( final TaskException te )
            {
                reportError( te );
            }

            final String message = REZ.getString( "repeat.notice" );
            System.out.println( message );

            if( null == reader )
            {
                reader = new BufferedReader( new InputStreamReader( System.in ) );
            }

            String line = reader.readLine();

            if( line.equalsIgnoreCase( "no" ) )
            {
                break;
            }
        }
    }

    /**
     * Builds the error message for an exception
     */
    private void reportError( final Throwable throwable )
    {
        // Build the message
        final String message;
        if( m_priority <= BasicLogger.LEVEL_INFO )
        {
            // Verbose mode - include the stack traces
            message = ExceptionUtil.printStackTrace( throwable, 5, true, true );
        }
        else
        {
            // Build the message
            final StringBuffer buffer = new StringBuffer();
            buffer.append( throwable.getMessage() );
            for( Throwable current = ExceptionUtil.getCause( throwable, true );
                 current != null;
                 current = ExceptionUtil.getCause( current, true ) )
            {
                final String causeMessage = REZ.getString( "cause.error", current.getMessage() );
                buffer.append( causeMessage );
            }
            message = buffer.toString();
        }

        // Write the message out
        if( getLogger() == null )
        {
            System.err.println( message );
        }
        else
        {
            getLogger().error( message );
        }
    }

    private void executeBuild( final Embeddor embeddor,
                               final Project project,
                               final ProjectListener listener )
        throws Exception
    {
        //actually do the build ...
        final Workspace workspace = embeddor.createWorkspace( m_defines );
        workspace.addProjectListener( listener );

        doBuild( workspace, project, m_targets );
    }

    private File getBuildFile() throws Exception
    {
        final String filename = m_parameters.getParameter( "filename", null );
        final File buildFile = ( new File( filename ) ).getCanonicalFile();
        if( !buildFile.isFile() )
        {
            final String message = REZ.getString( "bad-file.error", buildFile );
            throw new Exception( message );
        }

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "buildfile.notice", buildFile );
            getLogger().info( message );
        }

        return buildFile;
    }

    private void checkHomeDir() throws Exception
    {
        final String home = m_parameters.getParameter( "myrmidon.home" );
        final File homeDir = ( new File( home ) ).getAbsoluteFile();
        if( !homeDir.isDirectory() )
        {
            final String message = REZ.getString( "home-not-dir.error", homeDir );
            throw new Exception( message );
        }

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "homedir.notice", homeDir );
            getLogger().info( message );
        }
    }

    private void prepareLogging() throws Exception
    {
        //handle logging...
        final BasicLogger logger = new BasicLogger( "[myrmidon] ", m_priority );
        enableLogging( logger );
    }

    private void shutdownEmbeddor( final Embeddor embeddor )
        throws Exception
    {
        embeddor.stop();
        embeddor.dispose();
    }

    private ProjectListener prepareListener( final Embeddor embeddor )
        throws Exception
    {
        //create the listener
        final String listenerName = m_parameters.getParameter( "listener", null );
        final ProjectListener listener = embeddor.createListener( listenerName );
        return listener;
    }

    private Embeddor prepareEmbeddor()
        throws Exception
    {
        final Embeddor embeddor = createEmbeddor();
        setupLogger( embeddor );
        embeddor.parameterize( m_parameters );
        embeddor.initialize();
        embeddor.start();
        return embeddor;
    }

    private Embeddor createEmbeddor()
        throws Exception
    {
        final Class clazz = Class.forName( DEFAULT_EMBEDDOR_CLASS );
        return (Embeddor)clazz.newInstance();
    }

    /**
     * Actually do the build.
     *
     * @param workspace the workspace
     * @param project the project
     * @param targets the targets to build as passed by CLI
     */
    private void doBuild( final Workspace workspace,
                          final Project project,
                          final ArrayList targets )
        throws TaskException
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

    /**
     * Sets the log level.
     */
    private int mapLogLevel( final String logLevel )
        throws Exception
    {
        final String logLevelCapitalized = logLevel.toUpperCase();
        if( "DEBUG".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_DEBUG;
        }
        else if( "INFO".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_INFO;
        }
        else if( "WARN".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_WARN;
        }
        else if( "ERROR".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_ERROR;
        }
        else
        {
            final String message = REZ.getString( "bad-loglevel.error", logLevel );
            throw new Exception( message );
        }
    }
}
