/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.antlib.cvslib;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.exec.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;

/**
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:costin@dnt.ro">costin@dnt.ro</a>
 * @author <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:wwerner@picturesafe.de">Wolfgang Werner [wwerner@picturesafe.de]</a>
 * @version $Revision$ $Date$
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
     * If true it will stop the build if cvs exits with error. Default is false.
     * (Iulian)
     */
    private boolean m_failOnError;

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

    public void setNoexec( boolean ne )
    {
        m_noexec = ne;
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
        final Environment env = buildEnvironment();

        //FIXME:
        ExecuteStreamHandler streamhandler =
            new LogStreamHandler( null, Project.MSG_INFO, Project.MSG_WARN );

        final Execute exe = new Execute( streamhandler, null );
        if( m_dest == null ) m_dest = getBaseDirectory();
        exe.setWorkingDirectory( m_dest );

        exe.setCommandline( command.getCommandline() );
        exe.setEnvironment( env.getVariables() );
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

    private Environment buildEnvironment()
    {
        final Environment env = new Environment();
        if( 0 < m_port )
        {
            final Environment.Variable var = new Environment.Variable();
            var.setKey( "CVS_CLIENT_PORT" );
            var.setValue( String.valueOf( m_port ) );
            env.addVariable( var );
        }

        if( null != m_passwordFile )
        {
            final Environment.Variable var = new Environment.Variable();
            var.setKey( "CVS_PASSFILE" );
            var.setValue( String.valueOf( m_passwordFile ) );
            env.addVariable( var );
        }

        if( null != m_cvsRsh )
        {
            final Environment.Variable var = new Environment.Variable();
            var.setKey( "CVS_RSH" );
            var.setValue( String.valueOf( m_cvsRsh ) );
            env.addVariable( var );
        }
        return env;
    }

    private Commandline buildCommandline() throws TaskException
    {
        final Commandline command = new Commandline();

        command.setExecutable( "cvs" );
        if( m_cvsRoot != null )
        {
            command.createArgument().setValue( "-d" );
            command.createArgument().setValue( m_cvsRoot );
        }

        if( m_noexec )
        {
            command.createArgument().setValue( "-n" );
        }

        if( m_quiet )
        {
            command.createArgument().setValue( "-q" );
        }

        command.createArgument().setLine( m_command );

        if( null != m_date )
        {
            command.createArgument().setValue( "-D" );
            command.createArgument().setValue( m_date );
        }

        if( null != m_tag )
        {
            command.createArgument().setValue( "-r" );
            command.createArgument().setValue( m_tag );
        }

        if( m_module != null )
        {
            command.createArgument().setLine( m_module );
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

