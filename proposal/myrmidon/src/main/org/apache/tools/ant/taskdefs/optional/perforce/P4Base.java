/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.types.Commandline;

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
 * @see org.apache.tools.ant.taskdefs.Exec
 */
public abstract class P4Base extends org.apache.tools.ant.Task
{

    /**
     * Perl5 regexp in Java - cool eh?
     */
    protected Perl5Util util = null;

    //P4 runtime directives
    /**
     * Perforce Server Port (eg KM01:1666)
     */
    protected String P4Port = "";
    /**
     * Perforce Client (eg myclientspec)
     */
    protected String P4Client = "";
    /**
     * Perforce User (eg fbloggs)
     */
    protected String P4User = "";
    /**
     * Perforce view for commands (eg //projects/foobar/main/source/... )
     */
    protected String P4View = "";

    //P4 g-opts and cmd opts (rtfm)
    /**
     * Perforce 'global' opts. Forms half of low level API
     */
    protected String P4Opts = "";
    /**
     * Perforce command opts. Forms half of low level API
     */
    protected String P4CmdOpts = "";
    /**
     * The OS shell to use (cmd.exe or /bin/sh)
     */
    protected String shell;

    public void setClient( String P4Client )
    {
        this.P4Client = "-c" + P4Client;
    }

    public void setCmdopts( String P4CmdOpts )
    {
        this.P4CmdOpts = P4CmdOpts;
    }

    //Setters called by Ant
    public void setPort( String P4Port )
    {
        this.P4Port = "-p" + P4Port;
    }

    public void setUser( String P4User )
    {
        this.P4User = "-u" + P4User;
    }

    public void setView( String P4View )
    {
        this.P4View = P4View;
    }

    private void prepare()
    {
        util = new Perl5Util();

        //Get default P4 settings from environment - Mark would have done something cool with
        //introspection here.....:-)
        String tmpprop;
        if( ( tmpprop = getProject().getProperty( "p4.port" ) ) != null )
            setPort( tmpprop );
        if( ( tmpprop = getProject().getProperty( "p4.client" ) ) != null )
            setClient( tmpprop );
        if( ( tmpprop = getProject().getProperty( "p4.user" ) ) != null )
            setUser( tmpprop );
    }

    protected void execP4Command( String command )
        throws TaskException
    {
        execP4Command( command, null );
    }

    public void execute()
        throws TaskException
    {
        //Setup task before executing it
        prepare();
        super.execute();
    }

    /**
     * Execute P4 command assembled by subclasses.
     *
     * @param command The command to run
     * @param handler A P4Handler to process any input and output
     * @exception TaskException Description of Exception
     */
    protected void execP4Command( String command, P4Handler handler )
        throws TaskException
    {
        try
        {

            Commandline commandline = new Commandline();
            commandline.setExecutable( "p4" );

            //Check API for these - it's how CVS does it...
            if( P4Port != null && P4Port.length() != 0 )
            {
                commandline.createArgument().setValue( P4Port );
            }
            if( P4User != null && P4User.length() != 0 )
            {
                commandline.createArgument().setValue( P4User );
            }
            if( P4Client != null && P4Client.length() != 0 )
            {
                commandline.createArgument().setValue( P4Client );
            }
            commandline.createArgument().setLine( command );

            String[] cmdline = commandline.getCommandline();
            String cmdl = "";
            for( int i = 0; i < cmdline.length; i++ )
            {
                cmdl += cmdline[ i ] + " ";
            }

            log( "Execing " + cmdl, Project.MSG_VERBOSE );

            if( handler == null )
                handler = new SimpleP4OutputHandler( this );

            final Execute exe = new Execute( handler );
            exe.setCommandline( commandline.getCommandline() );

            try
            {
                exe.execute();
            }
            catch( IOException e )
            {
                throw new TaskException( "Error", e );
            }
            finally
            {
                try
                {
                    handler.stop();
                }
                catch( Exception e )
                {
                }
            }

        }
        catch( Exception e )
        {
            throw new TaskException( "Problem exec'ing P4 command: " + e.getMessage() );
        }
    }
}
