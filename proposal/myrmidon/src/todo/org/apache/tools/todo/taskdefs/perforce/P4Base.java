/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.perforce;

import org.apache.aut.nativelib.ExecManager;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.Execute;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.todo.types.Commandline;

/**
 * Base class for Perforce (P4) ANT tasks. See individual task for example
 * usage.
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 * @see P4Sync
 * @see P4Have
 * @see P4Change
 * @see P4Edit
 * @see P4Submit
 * @see P4Label
 */
public abstract class P4Base
    extends AbstractTask
    implements ExecOutputHandler
{
    /**
     * Perl5 regexp in Java - cool eh?
     */
    protected Perl5Util util;

    //P4 runtime directives
    /**
     * Perforce Server Port (eg KM01:1666)
     */
    protected String m_p4Port = "";
    /**
     * Perforce Client (eg myclientspec)
     */
    protected String m_p4Client = "";
    /**
     * Perforce User (eg fbloggs)
     */
    protected String m_p4User = "";
    /**
     * Perforce view for commands (eg //projects/foobar/main/source/... )
     */
    protected String m_p4View = "";

    //P4 g-opts and cmd opts (rtfm)
    /**
     * Perforce 'global' opts. Forms half of low level API
     */
    protected String P4Opts = "";
    /**
     * Perforce command opts. Forms half of low level API
     */
    protected String m_p4CmdOpts = "";
    /**
     * The OS shell to use (cmd.exe or /bin/sh)
     */
    protected String shell;

    private TaskException m_error;

    public void setClient( String P4Client )
    {
        this.m_p4Client = "-c" + P4Client;
    }

    public void setCmdopts( String P4CmdOpts )
    {
        this.m_p4CmdOpts = P4CmdOpts;
    }

    //Setters called by Ant
    public void setPort( String P4Port )
    {
        this.m_p4Port = "-p" + P4Port;
    }

    public void setUser( String P4User )
    {
        this.m_p4User = "-u" + P4User;
    }

    public void setView( String P4View )
    {
        this.m_p4View = P4View;
    }

    private void prepare()
    {
        util = new Perl5Util();

        //Get default P4 settings from environment - Mark would have done something cool with
        //introspection here.....:-)
        Object tmpprop;
        if( ( tmpprop = getContext().getProperty( "p4.port" ) ) != null )
        {
            setPort( tmpprop.toString() );
        }
        if( ( tmpprop = getContext().getProperty( "p4.client" ) ) != null )
        {
            setClient( tmpprop.toString() );
        }
        if( ( tmpprop = getContext().getProperty( "p4.user" ) ) != null )
        {
            setUser( tmpprop.toString() );
        }
    }

    public void execute()
        throws TaskException
    {
        //Setup task before executing it
        prepare();
    }

    /**
     * Execute P4 command assembled by subclasses.
     */
    protected void execP4Command( final String command,
                                  ExecOutputHandler handler )
        throws TaskException
    {
        try
        {
            final Commandline cmd = new Commandline();
            cmd.setExecutable( "p4" );

            //Check API for these - it's how CVS does it...
            if( m_p4Port != null && m_p4Port.length() != 0 )
            {
                cmd.addArgument( m_p4Port );
            }
            if( m_p4User != null && m_p4User.length() != 0 )
            {
                cmd.addArgument( m_p4User );
            }
            if( m_p4Client != null && m_p4Client.length() != 0 )
            {
                cmd.addArgument( m_p4Client );
            }
            cmd.addLine( command );

            if( handler == null )
            {
                handler = this;
            }

            final Execute exe = new Execute();
            exe.setExecOutputHandler( handler );
            exe.setCommandline( cmd );

            exe.execute( getContext() );
            if( null != m_error )
            {
                throw m_error;
            }
        }
        catch( TaskException te )
        {
            throw te;
        }
        catch( Exception e )
        {
            throw new TaskException( "Problem exec'ing P4 command: " + e.getMessage() );
        }
    }

    protected final void registerError( final TaskException error )
    {
        m_error = error;
        m_error.fillInStackTrace();
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        if( util.match( "/^exit/", line ) )
        {
            return;
        }

        //Throw exception on errors (except up-to-date)
        //p4 -s is unpredicatable. For example a server down
        //does not return error: markup
        //
        //Some forms producing commands (p4 -s change -o) do tag the output
        //others don't.....
        //Others mark errors as info, for example edit a file
        //which is already open for edit.....
        //Just look for error: - catches most things....

        if( util.match( "/error:/", line ) && !util.match( "/up-to-date/", line ) )
        {
            registerError( new TaskException( line ) );
        }

        getContext().info( util.substitute( "s/^.*: //", line ) );
    }

    public void stderr( final String line )
    {
    }
}
