/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.vss;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Task to perform CheckIn commands to Microsoft Visual Source Safe.
 *
 * @author Martin Poeschl
 */
public class MSVSSCHECKIN
    extends MSVSS
{
    private String m_localPath;
    private boolean m_recursive;
    private boolean m_writable;
    private String m_autoResponse;
    private String m_comment = "-";

    /**
     * Set behaviour, used in get command to make files that are 'got' writable
     */
    public final void setWritable( final boolean writable )
    {
        m_writable = writable;
    }

    public void setAutoresponse( final String autoResponse )
    {
        if( autoResponse.equals( "" ) || autoResponse.equals( "null" ) )
        {
            m_autoResponse = null;
        }
        else
        {
            m_autoResponse = autoResponse;
        }
    }

    /**
     * Set the comment to apply in SourceSafe <p>
     *
     * If this is null or empty, it will be replaced with "-" which is what
     * SourceSafe uses for an empty comment.
     */
    public void setComment( final String comment )
    {
        if( comment.equals( "" ) || comment.equals( "null" ) )
        {
            m_comment = "-";
        }
        else
        {
            m_comment = comment;
        }
    }

    /**
     * Set the local path.
     */
    public void setLocalpath( final Path localPath )
    {
        m_localPath = localPath.toString();
    }

    /**
     * Set behaviour recursive or non-recursive
     */
    public void setRecursive( final boolean recursive )
    {
        m_recursive = recursive;
    }

    /**
     * Checks the value set for the autoResponse. if it equals "Y" then we
     * return -I-Y if it equals "N" then we return -I-N otherwise we return -I
     */
    public void getAutoresponse( final Commandline cmd )
    {
        if( null == m_autoResponse )
        {
            cmd.addArgument( FLAG_AUTORESPONSE_DEF );
        }
        else if( m_autoResponse.equalsIgnoreCase( "Y" ) )
        {
            cmd.addArgument( FLAG_AUTORESPONSE_YES );

        }
        else if( m_autoResponse.equalsIgnoreCase( "N" ) )
        {
            cmd.addArgument( FLAG_AUTORESPONSE_NO );
        }
        else
        {
            cmd.addArgument( FLAG_AUTORESPONSE_DEF );
        }
    }

    /**
     * Builds and returns the -GL flag command if required <p>
     *
     * The localpath is created if it didn't exist
     */
    private void getLocalpathCommand( final Commandline cmd )
        throws TaskException
    {
        if( m_localPath == null )
        {
            return;
        }
        else
        {
            // make sure m_LocalDir exists, create it if it doesn't
            final File dir = getContext().resolveFile( m_localPath );
            if( !dir.exists() )
            {
                final boolean done = dir.mkdirs();
                if( !done )
                {
                    final String message =
                        "Directory " + m_localPath + " creation was not " +
                        "succesful for an unknown reason";
                    throw new TaskException( message );
                }

                final String message = "Created dir: " + dir.getAbsolutePath();
                getContext().info( message );
            }

            cmd.addArgument( FLAG_OVERRIDE_WORKING_DIR + m_localPath );
        }
    }

    private void getRecursiveCommand( final Commandline cmd )
    {
        if( !m_recursive )
        {
            return;
        }
        else
        {
            cmd.addArgument( FLAG_RECURSION );
        }
    }

    private void getWritableCommand( final Commandline cmd )
    {
        if( !m_writable )
        {
            return;
        }
        else
        {
            cmd.addArgument( FLAG_WRITABLE );
        }
    }

    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute ss and then calls Exec's run method to
     * execute the command line.
     */
    public void execute()
        throws TaskException
    {
        final Commandline commandLine = new Commandline();

        // first off, make sure that we've got a command and a vssdir ...
        final String vsspath = getVsspath();
        if( null == vsspath )
        {
            final String message = "vsspath attribute must be set!";
            throw new TaskException( message );
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss Checkin VSS items [-H] [-C] [-I-] [-N] [-O] [-R] [-W] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable( getSSCommand() );
        commandLine.addArgument( COMMAND_CHECKIN );

        // VSS items
        commandLine.addArgument( vsspath );
        // -GL
        getLocalpathCommand( commandLine );
        // -I- or -I-Y or -I-N
        getAutoresponse( commandLine );
        // -R
        getRecursiveCommand( commandLine );
        // -W
        getWritableCommand( commandLine );
        // -Y
        getLoginCommand( commandLine );
        // -C
        commandLine.addArgument( "-C" + m_comment );

        final int result = run( commandLine );
        if( result != 0 )
        {
            final String message = "Failed executing: " + commandLine.toString();
            throw new TaskException( message );
        }
    }
}
