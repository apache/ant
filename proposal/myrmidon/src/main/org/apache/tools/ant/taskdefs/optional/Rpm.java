/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.exec.Execute;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.exec.LogOutputStream;
import org.apache.tools.ant.taskdefs.exec.LogStreamHandler;
import org.apache.tools.ant.taskdefs.exec.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 * @author lucas@collab.net
 */
public class Rpm extends Task
{

    /**
     * the rpm command to use
     */
    private String command = "-bb";

    /**
     * clean BUILD directory
     */
    private boolean cleanBuildDir = false;

    /**
     * remove spec file
     */
    private boolean removeSpec = false;

    /**
     * remove sources
     */
    private boolean removeSource = false;

    /**
     * the file to direct standard error from the command
     */
    private File error;

    /**
     * the file to direct standard output from the command
     */
    private File output;

    /**
     * the spec file
     */
    private String specFile;

    /**
     * the rpm top dir
     */
    private File topDir;

    public void setCleanBuildDir( boolean cbd )
    {
        cleanBuildDir = cbd;
    }

    public void setCommand( String c )
    {
        this.command = c;
    }

    public void setError( File error )
    {
        this.error = error;
    }

    public void setOutput( File output )
    {
        this.output = output;
    }

    public void setRemoveSource( boolean rs )
    {
        removeSource = rs;
    }

    public void setRemoveSpec( boolean rs )
    {
        removeSpec = rs;
    }

    public void setSpecFile( String sf )
        throws TaskException
    {
        if( ( sf == null ) || ( sf.trim().equals( "" ) ) )
        {
            throw new TaskException( "You must specify a spec file" );
        }
        this.specFile = sf;
    }

    public void setTopDir( File td )
    {
        this.topDir = td;
    }

    public void execute()
        throws TaskException
    {

        Commandline toExecute = new Commandline();

        toExecute.setExecutable( "rpm" );
        if( topDir != null )
        {
            toExecute.createArgument().setValue( "--define" );
            toExecute.createArgument().setValue( "_topdir" + topDir );
        }

        toExecute.createArgument().setLine( command );

        if( cleanBuildDir )
        {
            toExecute.createArgument().setValue( "--clean" );
        }
        if( removeSpec )
        {
            toExecute.createArgument().setValue( "--rmspec" );
        }
        if( removeSource )
        {
            toExecute.createArgument().setValue( "--rmsource" );
        }

        toExecute.createArgument().setValue( "SPECS/" + specFile );

        ExecuteStreamHandler streamhandler = null;
        OutputStream outputstream = null;
        OutputStream errorstream = null;
        if( error == null && output == null )
        {
            streamhandler = new LogStreamHandler( this, Project.MSG_INFO,
                                                  Project.MSG_WARN );
        }
        else
        {
            if( output != null )
            {
                try
                {
                    outputstream = new PrintStream( new BufferedOutputStream( new FileOutputStream( output ) ) );
                }
                catch( IOException e )
                {
                    throw new TaskException( "Error", e );
                }
            }
            else
            {
                outputstream = new LogOutputStream( this, Project.MSG_INFO );
            }
            if( error != null )
            {
                try
                {
                    errorstream = new PrintStream( new BufferedOutputStream( new FileOutputStream( error ) ) );
                }
                catch( IOException e )
                {
                    throw new TaskException( "Error", e );
                }
            }
            else
            {
                errorstream = new LogOutputStream( this, Project.MSG_WARN );
            }
            streamhandler = new PumpStreamHandler( outputstream, errorstream );
        }

        Execute exe = new Execute( streamhandler );

        if( topDir == null ) topDir = getBaseDirectory();
        exe.setWorkingDirectory( topDir );

        exe.setCommandline( toExecute.getCommandline() );
        try
        {
            exe.execute();
            getLogger().info( "Building the RPM based on the " + specFile + " file" );
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
        finally
        {
            if( output != null )
            {
                try
                {
                    outputstream.close();
                }
                catch( IOException e )
                {
                }
            }
            if( error != null )
            {
                try
                {
                    errorstream.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }
}
