/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.unix;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.Execute2;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Argument;

/**
 * @author lucas@collab.net
 */
public class Rpm
    extends AbstractTask
{
    /**
     * the rpm command to use
     */
    private String m_command = "-bb";

    /**
     * clean BUILD directory
     */
    private boolean m_cleanBuildDir;

    /**
     * remove spec file
     */
    private boolean m_removeSpec;

    /**
     * remove sources
     */
    private boolean m_removeSource;

    /**
     * the spec file
     */
    private String m_specFile;

    /**
     * the rpm top dir
     */
    private File m_topDir;

    public void setCleanBuildDir( boolean cleanBuildDir )
    {
        m_cleanBuildDir = cleanBuildDir;
    }

    public void setCommand( final String command )
    {
        m_command = command;
    }

    public void setRemoveSource( final boolean removeSource )
    {
        m_removeSource = removeSource;
    }

    public void setRemoveSpec( final boolean removeSpec )
    {
        m_removeSpec = removeSpec;
    }

    public void setSpecFile( final String specFile )
        throws TaskException
    {
        if( ( specFile == null ) || ( specFile.trim().equals( "" ) ) )
        {
            throw new TaskException( "You must specify a spec file" );
        }
        m_specFile = specFile;
    }

    public void setTopDir( final File topDir )
    {
        m_topDir = topDir;
    }

    public void execute()
        throws TaskException
    {
        final Commandline cmd = createCommand();
        final Execute2 exe = new Execute2();
        setupLogger( exe );

        if( m_topDir == null ) m_topDir = getBaseDirectory();
        exe.setWorkingDirectory( m_topDir );

        exe.setCommandline( cmd.getCommandline() );
        try
        {
            final String message = "Building the RPM based on the " + m_specFile + " file";
            getLogger().info( message );

            if( 0 != exe.execute() )
            {
                throw new TaskException( "Failed to execute rpm" );
            }
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
    }

    private Commandline createCommand()
        throws TaskException
    {
        final Commandline cmd = new Commandline();
        cmd.setExecutable( "rpm" );
        if( m_topDir != null )
        {
            cmd.addArgument( "--define" );
            cmd.addArgument( "_topdir" + m_topDir );
        }

        cmd.addLine( m_command );

        if( m_cleanBuildDir )
        {
            cmd.addArgument( "--clean" );
        }
        if( m_removeSpec )
        {
            cmd.addArgument( "--rmspec" );
        }
        if( m_removeSource )
        {
            cmd.addArgument( "--rmsource" );
        }

        cmd.addArgument( "SPECS/" + m_specFile );
        return cmd;
    }
}
