/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.Execute2;
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
     *
     * @param ignore The new Ignorewhitespace value
     */
    public void setIgnorewhitespace( final boolean ignorewhitespace )
    {
        m_ignorewhitespace = ignorewhitespace;
    }

    /**
     * The file to patch.
     *
     * @param file The new Originalfile value
     */
    public void setOriginalfile( final File originalFile )
    {
        m_originalFile = originalFile;
    }

    /**
     * The file containing the diff output.
     *
     * @param file The new Patchfile value
     */
    public void setPatchfile( final File patchFile )
    {
        m_patchFile = patchFile;
    }

    /**
     * Work silently unless an error occurs.
     *
     * @param q The new Quiet value
     */
    public void setQuiet( final boolean quiet )
    {
        m_quiet = quiet;
    }

    /**
     * Assume patch was created with old and new files swapped.
     *
     * @param r The new Reverse value
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

        final Execute2 exe = new Execute2();
        setupLogger( exe );

        final Commandline cmd = buildCommand();
        exe.setCommandline( cmd );

        try
        {
            exe.execute();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
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

    private Commandline buildCommand()
    {
        final Commandline cmd = new Commandline();
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
        return cmd;
    }
}
