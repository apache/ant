/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.vss;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.Path;

/**
 * Task to perform CheckOut commands to Microsoft Visual Source Safe.
 *
 * @author Martin Poeschl
 */
public class MSVSSCHECKOUT
    extends MSVSS
{
    private String m_localPath;
    private boolean m_recursive;
    private String m_version;
    private String m_date;
    private String m_label;
    private String m_autoResponse;

    public void setAutoresponse( final String response )
    {
        if( response.equals( "" ) || response.equals( "null" ) )
        {
            m_autoResponse = null;
        }
        else
        {
            m_autoResponse = response;
        }
    }

    /**
     * Set the stored date string <p>
     *
     * Note we assume that if the supplied string has the value "null" that
     * something went wrong and that the string value got populated from a null
     * object. This happens if a ant variable is used e.g. date="${date}" when
     * date has not been defined to ant!
     */
    public void setDate( final String date )
    {
        if( date.equals( "" ) || date.equals( "null" ) )
        {
            m_date = null;
        }
        else
        {
            m_date = date;
        }
    }

    /**
     * Set the labeled version to operate on in SourceSafe <p>
     *
     * Note we assume that if the supplied string has the value "null" that
     * something went wrong and that the string value got populated from a null
     * object. This happens if a ant variable is used e.g.
     * label="${label_server}" when label_server has not been defined to ant!
     */
    public void setLabel( final String label )
    {
        if( label.equals( "" ) || label.equals( "null" ) )
        {
            m_label = null;
        }
        else
        {
            m_label = label;
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
     * Set the stored version string <p>
     *
     * Note we assume that if the supplied string has the value "null" that
     * something went wrong and that the string value got populated from a null
     * object. This happens if a ant variable is used e.g.
     * version="${ver_server}" when ver_server has not been defined to ant!
     */
    public void setVersion( final String version )
    {
        if( version.equals( "" ) || version.equals( "null" ) )
        {
            m_version = null;
        }
        else
        {
            m_version = version;
        }
    }

    /**
     * Checks the value set for the autoResponse. if it equals "Y" then we
     * return -I-Y if it equals "N" then we return -I-N otherwise we return -I
     */
    public void getAutoresponse( final Commandline cmd )
    {
        if( m_autoResponse == null )
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
    public void getLocalpathCommand( final Commandline cmd )
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
                if( !dir.mkdirs() )
                {
                    final String message =
                        "Directory " + m_localPath + " creation was not " +
                        "succesful for an unknown reason";
                    throw new TaskException( message );
                }
                else
                {
                    final String message = "Created dir: " + dir.getAbsolutePath();
                    getContext().info( message );
                }
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

    /**
     * Simple order of priority. Returns the first specified of version, date,
     * label If none of these was specified returns ""
     */
    private void getVersionCommand( final Commandline cmd )
    {
        if( null != m_version )
        {
            cmd.addArgument( FLAG_VERSION + m_version );
        }
        else if( null != m_date )
        {
            cmd.addArgument( FLAG_VERSION_DATE + m_date );
        }
        else if( null != m_label )
        {
            cmd.addArgument( FLAG_VERSION_LABEL + m_label );
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
        // ss Checkout VSS items [-G] [-C] [-H] [-I-] [-N] [-O] [-R] [-V] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable( getSSCommand() );
        commandLine.addArgument( COMMAND_CHECKOUT );

        // VSS items
        commandLine.addArgument( vsspath );
        // -GL
        getLocalpathCommand( commandLine );
        // -I- or -I-Y or -I-N
        getAutoresponse( commandLine );
        // -R
        getRecursiveCommand( commandLine );
        // -V
        getVersionCommand( commandLine );
        // -Y
        getLoginCommand( commandLine );

        final int result = run( commandLine );
        if( result != 0 )
        {
            final String message = "Failed executing: " + commandLine.toString();
            throw new TaskException( message );
        }
    }
}

