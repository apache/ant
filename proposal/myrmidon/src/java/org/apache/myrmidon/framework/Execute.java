/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.io.File;
import java.util.Properties;
import org.apache.aut.nativelib.ExecException;
import org.apache.aut.nativelib.ExecManager;
import org.apache.aut.nativelib.ExecMetaData;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.util.FileUtils;

/**
 * This is a utility class designed to make executing native
 * processes easier in the context of ant.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public class Execute
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( Execute.class );

    private Commandline m_command;
    private Properties m_environment = new Properties();
    private File m_workingDirectory = new File( "." );
    private boolean m_newEnvironment;
    private ExecOutputHandler m_handler;
    private long m_timeout;
    private Integer m_returnCode;

    public void setTimeout( final long timeout )
    {
        m_timeout = timeout;
    }

    public void setExecOutputHandler( final ExecOutputHandler handler )
    {
        m_handler = handler;
    }

    /**
     * Sets the commandline of the subprocess to launch.
     *
     * @param command the commandline of the subprocess to launch
     */
    public void setCommandline( final Commandline command )
    {
        m_command = command;
    }

    public Commandline getCommandline()
    {
        if( null == m_command )
        {
            m_command = new Commandline();
        }
        return m_command;
    }

    public void setEnvironment( final Properties environment )
    {
        if( null == environment )
        {
            throw new NullPointerException( "environment" );
        }
        m_environment = environment;
    }

    /**
     * If this variable is false then then the environment specified is
     * added to the environment variables for current process. If this
     * value is true then the specified environment replaces the environment
     * for the command.
     */
    public void setNewenvironment( final boolean newEnvironment )
    {
        m_newEnvironment = newEnvironment;
    }

    /**
     * Sets the working directory of the process to execute. <p>
     *
     * @param workingDirectory the working directory of the process.
     */
    public void setWorkingDirectory( final File workingDirectory )
    {
        m_workingDirectory = workingDirectory;
    }

    public void setReturnCode( final int returnCode )
    {
        m_returnCode = new Integer( returnCode );
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess.
     */
    public int execute( final TaskContext context )
        throws TaskException
    {
        validate();

        try
        {
            // Build an output handler
            final ExecOutputHandler handler = buildOutputHandler( context );

            // Build the command meta-info
            final ExecManager execManager = (ExecManager)context.getService( ExecManager.class );
            final ExecMetaData metaData = buildExecMetaData( execManager );

            logExecDetails( metaData, context );

            // Execute the command and check return code
            final int returnCode = execManager.execute( metaData, handler, m_timeout );
            checkReturnCode( returnCode );
            return returnCode;
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "execute.failed.error", m_command.getExecutable() );
            throw new TaskException( message, e );
        }
    }

    /**
     * Logs the details of the command.
     */
    private void logExecDetails( final ExecMetaData metaData,
                                 final TaskContext context )
        throws TaskException
    {
        if( ! context.isDebugEnabled() )
        {
            return;
        }

        String cmdline = FileUtils.buildCommandLine( metaData.getCommand() );
        String message = REZ.getString( "execute.command.notice", cmdline );
        context.debug( message );
        message = REZ.getString( "execute.env-vars.notice", metaData.getEnvironment() );
        context.debug( message );
    }

    /**
     * Vaidates the arguments.
     */
    private void validate() throws TaskException
    {
        if( null == m_command.getExecutable() )
        {
            final String message = REZ.getString( "execute.no-executable.error" );
            throw new TaskException( message );
        }
        if( !m_workingDirectory.exists() )
        {
            final String message = REZ.getString( "execute.dir-noexist.error", m_workingDirectory );
            throw new TaskException( message );
        }
        else if( !m_workingDirectory.isDirectory() )
        {
            final String message = REZ.getString( "execute.dir-notdir.error", m_workingDirectory );
            throw new TaskException( message );
        }
    }

    /**
     * Creates an output handler to use when executing the commmand.
     */
    private ExecOutputHandler buildOutputHandler( final TaskContext context )
    {
        ExecOutputHandler handler = m_handler;
        if( handler == null )
        {
            handler = new LoggingExecOutputHandler( context );
        }
        return handler;
    }

    /**
     * Utility method to verify that specified return code was the
     * return code expected (if any).
     */
    private void checkReturnCode( final int returnCode )
        throws TaskException
    {
        if( null != m_returnCode &&
            returnCode != m_returnCode.intValue() )
        {
            final String message = REZ.getString( "execute.bad-resultcode.error",
                                                  m_command.getExecutable(),
                                                  new Integer(returnCode) );
            throw new TaskException( message );
        }
    }

    /**
     * Utility method to create an ExecMetaData object
     * to pass to the ExecManager service.
     */
    private ExecMetaData buildExecMetaData( final ExecManager execManager )
        throws ExecException
    {
        final String[] command = m_command.getCommandline();

        final Properties newEnvironment = new Properties();
        if( !m_newEnvironment )
        {
            newEnvironment.putAll( execManager.getNativeEnvironment() );
        }
        newEnvironment.putAll( m_environment );

        return new ExecMetaData( command,
                                 newEnvironment,
                                 m_workingDirectory );
    }
}
