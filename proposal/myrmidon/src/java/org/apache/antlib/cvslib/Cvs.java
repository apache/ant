/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.cvslib;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;

/**
 * Task to interact with a CVS repository.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:costin@dnt.ro">costin@dnt.ro</a>
 * @author <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:wwerner@picturesafe.de">Wolfgang Werner</a>
 * @version $Revision$ $Date$
 * @ant:task name="cvs"
 */
public class Cvs
    extends AbstractTask
{
    /**
     * the CVS command to execute.
     */
    private String m_command = "checkout";

    /**
     * suppress information messages.
     */
    private boolean m_quiet;

    /**
     * report only, don't change any files.
     */
    private boolean m_noexec;

    /**
     * CVS port
     */
    private int m_port;

    /**
     * CVS password file
     */
    private File m_passwordFile;

    /**
     * the CVSROOT variable.
     */
    private String m_cvsRoot;

    /**
     * the CVS_RSH variable.
     */
    private String m_cvsRsh;

    /**
     * the directory where the checked out files should be placed.
     */
    private File m_dest;

    /**
     * the module to check out.
     */
    private String m_module;

    /**
     * the date at which to extract files from repository
     */
    private String m_date;

    /**
     * the tag with which to extract files from the repository
     */
    private String m_tag;

    public void setCommand( final String command )
    {
        m_command = command;
    }

    public void setCvsRoot( final String cvsRoot )
    {
        // Check if not real cvsroot => set it to null
        m_cvsRoot = getNonEmptyString( cvsRoot );
    }

    public void setCvsRsh( final String cvsRsh )
    {
        // Check if not real cvsrsh => set it to null
        m_cvsRsh = getNonEmptyString( cvsRsh );
    }

    public void setDate( final String date )
    {
        m_date = getNonEmptyString( date );
    }

    public void setDest( final File dest )
    {
        m_dest = dest;
    }

    public void setNoexec( final boolean noexec )
    {
        m_noexec = noexec;
    }

    public void setModule( final String module )
    {
        m_module = module;
    }

    public void setPassfile( final File passwordFile )
    {
        m_passwordFile = passwordFile;
    }

    public void setPort( final int port )
    {
        m_port = port;
    }

    public void setQuiet( final boolean quiet )
    {
        m_quiet = quiet;
    }

    public void setTag( final String tag )
    {
        m_tag = getNonEmptyString( tag );
    }

    public void execute()
        throws TaskException
    {
        final Commandline command = buildCommandline();
        final Properties env = buildEnvironment();

        final ExecManager execManager = (ExecManager)getService( ExecManager.class );
        final Execute exe = new Execute( execManager );
        if( m_dest == null )
        {
            m_dest = getBaseDirectory();
        }
        exe.setWorkingDirectory( m_dest );

        exe.setCommandline( command );
        exe.setEnvironment( env );
        try
        {
            final int retCode = exe.execute();
            if( retCode != 0 )
            {
                //replace with an ExecuteException(message,code);
                throw new TaskException( "cvs exited with error code " + retCode );
            }
        }
        catch( IOException e )
        {
            throw new TaskException( e.toString(), e );
        }
    }

    private Properties buildEnvironment()
    {
        final Properties env = new Properties();
        if( 0 < m_port )
        {
            env.setProperty( "CVS_CLIENT_PORT", String.valueOf( m_port ) );
        }

        if( null != m_passwordFile )
        {
            env.setProperty( "CVS_PASSFILE", String.valueOf( m_passwordFile ) );
        }

        if( null != m_cvsRsh )
        {
            env.setProperty( "CVS_RSH", String.valueOf( m_cvsRsh ) );
        }
        return env;
    }

    private Commandline buildCommandline() throws TaskException
    {
        final Commandline command = new Commandline();

        command.setExecutable( "cvs" );
        if( m_cvsRoot != null )
        {
            command.addArgument( "-d" );
            command.addArgument( m_cvsRoot );
        }

        if( m_noexec )
        {
            command.addArgument( "-n" );
        }

        if( m_quiet )
        {
            command.addArgument( "-q" );
        }

        command.addArguments( FileUtils.translateCommandline( m_command ) );

        if( null != m_date )
        {
            command.addArgument( "-D" );
            command.addArgument( m_date );
        }

        if( null != m_tag )
        {
            command.addArgument( "-r" );
            command.addArgument( m_tag );
        }

        if( m_module != null )
        {
            command.addArguments( FileUtils.translateCommandline( m_module ) );
        }
        return command;
    }

    private String getNonEmptyString( final String value )
    {
        if( isEmpty( value ) )
        {
            return null;
        }
        else
        {
            return value;
        }
    }

    private boolean isEmpty( final String value )
    {
        return ( null == value ) || ( 0 == value.trim().length() );
    }
}

