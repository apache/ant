/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.nativelib;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.aut.nativelib.Os;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Argument;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnvironmentData;
import org.apache.tools.ant.types.EnvironmentVariable;

/**
 * Executes a native command.
 *
 * @author <a href="mailto:duncan@x180.com">JDD</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 */
public class Exec
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Exec.class );

    private long m_timeout;
    private EnvironmentData m_env = new EnvironmentData();
    private Commandline m_command = new Commandline();
    private boolean m_newEnvironment;
    private File m_dir;
    private String m_os;

    /**
     * The working directory of the process
     */
    public void setDir( final File dir )
        throws TaskException
    {
        m_dir = dir;
    }

    /**
     * The command to execute.
     */
    public void setExecutable( final String value )
        throws TaskException
    {
        m_command.setExecutable( value );
    }

    /**
     * Use a completely new environment
     */
    public void setNewenvironment( final boolean newEnvironment )
    {
        m_newEnvironment = newEnvironment;
    }

    /**
     * Only execute the process if <code>os.name</code> is included in this
     * string.
     */
    public void setOs( final String os )
    {
        m_os = os;
    }

    /**
     * Timeout in milliseconds after which the process will be killed.
     */
    public void setTimeout( final long timeout )
    {
        m_timeout = timeout;
    }

    /**
     * Add a nested env element - an environment variable.
     */
    public void addEnv( final EnvironmentVariable var )
    {
        m_env.addVariable( var );
    }

    /**
     * Add a nested arg element - a command line argument.
     */
    public void addArg( final Argument argument )
    {
        m_command.addArgument( argument );
    }

    public void execute()
        throws TaskException
    {
        validate();
        if( null == m_os || Os.isFamily( m_os ) )
        {
            final Execute2 exe = createExecute();
            doExecute( exe );
        }
    }

    private void doExecute( final Execute2 exe )
        throws TaskException
    {
        try
        {
            final int err = exe.execute();
            if( 0 != err )
            {
                final String message =
                    REZ.getString( "exec.bad-resultcode.error", new Integer( err ) );
                throw new TaskException( message );
            }
        }
        catch( final IOException ioe )
        {
            final String message = REZ.getString( "exec.failed.error", ioe );
            throw new TaskException( message, ioe );
        }
    }

    private void validate()
        throws TaskException
    {
        if( null == m_command.getExecutable() )
        {
            final String message = REZ.getString( "exec.no-executable.error" );
            throw new TaskException( message );
        }

        // default directory to the project's base directory
        if( m_dir == null )
        {
            m_dir = getBaseDirectory();
        }
        else
        {
            if( !m_dir.exists() )
            {
                final String message = REZ.getString( "exec.dir-noexist.error", m_dir );
                throw new TaskException( message );
            }
            else if( !m_dir.isDirectory() )
            {
                final String message = REZ.getString( "exec.dir-notdir.error", m_dir );
                throw new TaskException( message );
            }
        }
    }

    private Execute2 createExecute()
        throws TaskException
    {
        final Properties environment = m_env.getVariables();

        logExecDetails( environment );

        final Execute2 exe = new Execute2();
        setupLogger( exe );
        exe.setTimeout( m_timeout );
        exe.setWorkingDirectory( m_dir );
        exe.setNewenvironment( m_newEnvironment );
        exe.setEnvironment( environment );
        exe.setCommandline( m_command );
        return exe;
    }

    private void logExecDetails( final Properties environment )
    {
        // show the command
        getLogger().debug( m_command.toString() );
        final String message = REZ.getString( "exec.env-vars.notice", environment );
        getLogger().debug( message );
    }
}
