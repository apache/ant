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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.avalon.excalibur.cli.CLArgsParser;
import org.apache.avalon.excalibur.cli.CLOption;
import org.apache.avalon.excalibur.cli.CLOptionDescriptor;
import org.apache.avalon.excalibur.cli.CLUtil;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.CascadingException;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.myrmidon.Constants;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.executor.Executor;

/**
 * The class to kick the tires and light the fires.
 * Starts myrmidon, loads ProjectBuilder, builds project then uses ProjectManager
 * to run project.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class CLIMain
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
    private static final int NO_PREFIX_OPT = 'p';
    private static final int VERSION_OPT = 1;
    private static final int LISTENER_OPT = 2;
    private static final int TASKLIB_DIR_OPT = 5;
    private static final int EXTLIB_DIR_OPT = 6;
    private static final int INCREMENTAL_OPT = 7;
    private static final int HOME_DIR_OPT = 8;
    private static final int DRY_RUN_OPT = 9;
    private static final int DEBUG_OPT = 10;
    private static final int TYPE_OPT = 11;

    //incompatable options for info options
    private static final int[] INFO_OPT_INCOMPAT = new int[]
    {
        HELP_OPT, QUIET_OPT, VERBOSE_OPT, FILE_OPT,
        LOG_LEVEL_OPT, BUILDER_PARAM_OPT, NO_PREFIX_OPT,
        VERSION_OPT, LISTENER_OPT, TASKLIB_DIR_OPT, EXTLIB_DIR_OPT,
        INCREMENTAL_OPT, HOME_DIR_OPT, DRY_RUN_OPT, TYPE_OPT
    };

    //incompatable options for other logging options
    private static final int[] LOG_OPT_INCOMPAT = new int[]
    {
        QUIET_OPT, VERBOSE_OPT, LOG_LEVEL_OPT, DEBUG_OPT
    };

    //incompatible options for listener options
    private static final int[] LISTENER_OPT_INCOMPAT = new int[]
    {
        LISTENER_OPT, NO_PREFIX_OPT
    };

    ///List of targets supplied on command line to execute
    private ArrayList m_targets = new ArrayList();

    ///Determine whether tasks are actually executed
    private boolean m_dryRun = false;

    ///Enables incremental mode
    private boolean m_incremental;

    ///The launcher
    private EmbeddedAnt m_embedded = new EmbeddedAnt();

    ///Log level to use
    private static int m_priority = BasicLogger.LEVEL_WARN;

    /**
     * Main entry point called to run standard Myrmidon.
     *
     * @param args the args
     */
    public static void main( final String[] args )
    {
        final Map properties = new HashMap();
        properties.put( "myrmidon.home", new File( "." ) );
        main( properties, args );
    }

    /**
     * Main entry point called to run standard Myrmidon.
     *
     * @param args the args
     */
    public static void main( final Map properties, final String[] args )
    {
        int exitCode = 0;
        final CLIMain main = new CLIMain();
        try
        {
            main.execute( properties, args );
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
            new CLOptionDescriptor( "debug",
                                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                                    DEBUG_OPT,
                                    REZ.getString( "debug.opt" ),
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
            new CLOptionDescriptor( "antlib-path",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    TASKLIB_DIR_OPT,
                                    REZ.getString( "tasklib.opt" ) ),
            new CLOptionDescriptor( "ext-path",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    EXTLIB_DIR_OPT,
                                    REZ.getString( "extlib.opt" ) ),
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
                                    REZ.getString( "dry-run.opt" ) ),
            new CLOptionDescriptor( "type",
                                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                                    TYPE_OPT,
                                    REZ.getString( "type.opt" ) )
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
                    m_embedded.setEmbeddorProperty( "myrmidon.home", option.getArgument() );
                    break;
                case TASKLIB_DIR_OPT:
                    m_embedded.setEmbeddorProperty( "myrmidon.antlib.path", option.getArgument() );
                    break;
                case EXTLIB_DIR_OPT:
                    m_embedded.setEmbeddorProperty( "myrmidon.ext.path", option.getArgument() );
                    break;

                case LOG_LEVEL_OPT:
                    m_priority = mapLogLevel( option.getArgument() );
                    break;
                case VERBOSE_OPT:
                    m_priority = BasicLogger.LEVEL_INFO;
                    break;
                case DEBUG_OPT:
                    m_priority = BasicLogger.LEVEL_DEBUG;
                    break;
                case QUIET_OPT:
                    m_priority = BasicLogger.LEVEL_ERROR;
                    break;

                case INCREMENTAL_OPT:
                    m_incremental = true;
                    break;

                case FILE_OPT:
                    m_embedded.setProjectFile( option.getArgument() );
                    break;

                case LISTENER_OPT:
                    m_embedded.setProjectListener( option.getArgument() );
                    break;
                case NO_PREFIX_OPT:
                    m_embedded.setProjectListener( "noprefix" );
                    break;

                case DEFINE_OPT:
                    m_embedded.setWorkspaceProperty( option.getArgument( 0 ), option.getArgument( 1 ) );
                    break;

                case BUILDER_PARAM_OPT:
                    m_embedded.setBuilderProperty( option.getArgument( 0 ), option.getArgument( 1 ) );
                    break;

                case DRY_RUN_OPT:
                    m_dryRun = true;
                    break;

                case TYPE_OPT:
                    m_embedded.setProjectType( option.getArgument( 0 ) );
                    break;

                case 0:
                    m_targets.add( option.getArgument() );
                    break;
            }
        }

        return true;
    }

    private void execute( final Map properties, final String[] args )
        throws Exception
    {
        try
        {
            // Set system properties set up by launcher
            m_embedded.setHomeDirectory( (File)properties.get( "myrmidon.home" ) );

            // Command line
            if( !parseCommandLineOptions( args ) )
            {
                return;
            }

            // Setup logging
            final BasicLogger logger = new BasicLogger( "[myrmidon] ", m_priority );
            m_embedded.enableLogging( logger );

            if( m_dryRun )
            {
                m_embedded.setEmbeddorProperty( Executor.ROLE,
                                                "org.apache.myrmidon.components.executor.PrintingExecutor" );
            }

            // Set the common classloader
            final ClassLoader sharedClassLoader = (ClassLoader)properties.get( "myrmidon.shared.classloader" );
            m_embedded.setSharedClassLoader( sharedClassLoader );

            //loop over build if we are in incremental mode..
            if( !m_incremental )
            {
                executeBuild();
            }
            else
            {
                executeIncrementalBuild();
            }
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "build-failed.error" );
            throw new CascadingException( message, e );
        }
        finally
        {
            m_embedded.stop();
        }
    }

    private void executeIncrementalBuild()
        throws Exception
    {
        BufferedReader reader = null;

        while( true )
        {
            try
            {
                executeBuild();
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

    private void executeBuild() throws Exception
    {
        //actually do the build ...
        final String[] targets = (String[])m_targets.toArray( new String[ m_targets.size() ] );
        m_embedded.executeTargets( targets );
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
            message = ExceptionUtil.printStackTrace( throwable, 8, true, true );
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
        System.err.println( message );
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
        else if( "VERBOSE".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_INFO;
        }
        else if( "INFO".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_WARN;
        }
        else if( "WARN".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_ERROR;
        }
        else if( "ERROR".equals( logLevelCapitalized ) )
        {
            return BasicLogger.LEVEL_FATAL;
        }
        else
        {
            final String message = REZ.getString( "bad-loglevel.error", logLevel );
            throw new Exception( message );
        }
    }
}
