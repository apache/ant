/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.build;

import java.io.File;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.ant.types.Commandline;

/**
 * Task as a layer on top of patch. Patch applies a diff file to an original.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Patch
    extends AbstractTask
{
    private File m_originalFile;
    private File m_patchFile;
    private boolean m_backups;
    private boolean m_ignorewhitespace;
    private boolean m_reverse;
    private boolean m_quiet;
    private Integer m_strip;

    /**
     * Shall patch write backups.
     *
     * @param backups The new Backups value
     */
    public void setBackups( final boolean backups )
    {
        m_backups = backups;
    }

    /**
     * Ignore whitespace differences.
     */
    public void setIgnorewhitespace( final boolean ignorewhitespace )
    {
        m_ignorewhitespace = ignorewhitespace;
    }

    /**
     * The file to patch.
     */
    public void setOriginalfile( final File originalFile )
    {
        m_originalFile = originalFile;
    }

    /**
     * The file containing the diff output.
     */
    public void setPatchfile( final File patchFile )
    {
        m_patchFile = patchFile;
    }

    /**
     * Work silently unless an error occurs.
     */
    public void setQuiet( final boolean quiet )
    {
        m_quiet = quiet;
    }

    /**
     * Assume patch was created with old and new files swapped.
     */
    public void setReverse( final boolean reverse )
    {
        m_reverse = reverse;
    }

    /**
     * Strip the smallest prefix containing <i>num</i> leading slashes from
     * filenames. <p>
     *
     * patch's <i>-p</i> option.
     *
     * @param strip The new Strip value
     */
    public void setStrip( final Integer strip )
    {
        m_strip = strip;
    }

    public void execute()
        throws TaskException
    {
        validate();

        final ExecManager execManager = (ExecManager)getService( ExecManager.class );
        final Execute exe = new Execute( execManager );
        buildCommand( exe.getCommandline() );
        exe.execute();
    }

    private void validate()
        throws TaskException
    {
        if( null == m_patchFile )
        {
            final String message = "patchfile argument is required";
            throw new TaskException( message );
        }

        if( !m_patchFile.exists() )
        {
            final String message = "patchfile " + m_patchFile + " doesn\'t exist";
            throw new TaskException( message );
        }

        if( null != m_strip && m_strip.intValue() < 0 )
        {
            throw new TaskException( "strip has to be >= 0" );
        }
    }

    private void buildCommand( final Commandline cmd )
    {
        cmd.setExecutable( "patch" );
        if( m_backups )
        {
            cmd.addArgument( "-b" );
        }

        if( null != m_strip )
        {
            cmd.addArgument( "-p" + m_strip.intValue() );
        }

        if( m_quiet )
        {
            cmd.addArgument( "-s" );
        }

        if( m_reverse )
        {
            cmd.addArgument( "-R" );
        }

        cmd.addArgument( "-i" );
        cmd.addArgument( m_patchFile );

        if( m_ignorewhitespace )
        {
            cmd.addArgument( "-l" );
        }

        if( null != m_originalFile )
        {
            cmd.addArgument( m_originalFile );
        }
    }
}
