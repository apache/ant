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
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Task to perform CheckIn commands to Microsoft Visual Source Safe.
 *
 * @author Martin Poeschl
 */
public class MSVSSCHECKIN extends MSVSS
{

    private String m_LocalPath = null;
    private boolean m_Recursive = false;
    private boolean m_Writable = false;
    private String m_AutoResponse = null;
    private String m_Comment = "-";

    /**
     * Set behaviour, used in get command to make files that are 'got' writable
     *
     * @param argWritable The new Writable value
     */
    public final void setWritable( boolean argWritable )
    {
        m_Writable = argWritable;
    }

    public void setAutoresponse( String response )
    {
        if( response.equals( "" ) || response.equals( "null" ) )
        {
            m_AutoResponse = null;
        }
        else
        {
            m_AutoResponse = response;
        }
    }

    /**
     * Set the comment to apply in SourceSafe <p>
     *
     * If this is null or empty, it will be replaced with "-" which is what
     * SourceSafe uses for an empty comment.
     *
     * @param comment The new Comment value
     */
    public void setComment( String comment )
    {
        if( comment.equals( "" ) || comment.equals( "null" ) )
        {
            m_Comment = "-";
        }
        else
        {
            m_Comment = comment;
        }
    }

    /**
     * Set the local path.
     *
     * @param localPath The new Localpath value
     */
    public void setLocalpath( Path localPath )
    {
        m_LocalPath = localPath.toString();
    }

    /**
     * Set behaviour recursive or non-recursive
     *
     * @param recursive The new Recursive value
     */
    public void setRecursive( boolean recursive )
    {
        m_Recursive = recursive;
    }

    /**
     * Checks the value set for the autoResponse. if it equals "Y" then we
     * return -I-Y if it equals "N" then we return -I-N otherwise we return -I
     *
     * @param cmd Description of Parameter
     */
    public void getAutoresponse( Commandline cmd )
    {

        if( m_AutoResponse == null )
        {
            cmd.addArgument( FLAG_AUTORESPONSE_DEF );
        }
        else if( m_AutoResponse.equalsIgnoreCase( "Y" ) )
        {
            cmd.addArgument( FLAG_AUTORESPONSE_YES );

        }
        else if( m_AutoResponse.equalsIgnoreCase( "N" ) )
        {
            cmd.addArgument( FLAG_AUTORESPONSE_NO );
        }
        else
        {
            cmd.addArgument( FLAG_AUTORESPONSE_DEF );
        }// end of else

    }

    /**
     * Gets the comment to be applied.
     *
     * @return the comment to be applied.
     */
    public String getComment()
    {
        return m_Comment;
    }

    /**
     * Builds and returns the -GL flag command if required <p>
     *
     * The localpath is created if it didn't exist
     *
     * @param cmd Description of Parameter
     */
    public void getLocalpathCommand( Commandline cmd )
        throws TaskException
    {
        if( m_LocalPath == null )
        {
            return;
        }
        else
        {
            // make sure m_LocalDir exists, create it if it doesn't
            File dir = resolveFile( m_LocalPath );
            if( !dir.exists() )
            {
                boolean done = dir.mkdirs();
                if( done == false )
                {
                    String msg = "Directory " + m_LocalPath + " creation was not " +
                        "succesful for an unknown reason";
                    throw new TaskException( msg );
                }
                getLogger().info( "Created dir: " + dir.getAbsolutePath() );
            }

            cmd.addArgument( FLAG_OVERRIDE_WORKING_DIR + m_LocalPath );
        }
    }

    /**
     * @param cmd Description of Parameter
     */
    public void getRecursiveCommand( Commandline cmd )
    {
        if( !m_Recursive )
        {
            return;
        }
        else
        {
            cmd.addArgument( FLAG_RECURSION );
        }
    }

    /**
     * @param cmd Description of Parameter
     */
    public void getWritableCommand( Commandline cmd )
    {
        if( !m_Writable )
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
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        Commandline commandLine = new Commandline();
        int result = 0;

        // first off, make sure that we've got a command and a vssdir ...
        if( getVsspath() == null )
        {
            String msg = "vsspath attribute must be set!";
            throw new TaskException( msg );
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss Checkin VSS items [-H] [-C] [-I-] [-N] [-O] [-R] [-W] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable( getSSCommand() );
        commandLine.addArgument( COMMAND_CHECKIN );

        // VSS items
        commandLine.addArgument( getVsspath() );
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
        commandLine.addArgument( "-C" + getComment() );

        result = run( commandLine );
        if( result != 0 )
        {
            String msg = "Failed executing: " + commandLine.toString();
            throw new TaskException( msg );
        }
    }
}
