/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs;

import java.io.File;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.types.Commandline;

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
        final Execute exe = createCommand();
        exe.setWorkingDirectory( m_topDir );

        final String message = "Building the RPM based on the " + m_specFile + " file";
        getContext().info( message );
        exe.execute( getContext() );
    }

    private Execute createCommand()
        throws TaskException
    {
        final Execute cmd = new Execute();
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
