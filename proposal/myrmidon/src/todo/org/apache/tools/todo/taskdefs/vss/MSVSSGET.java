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
import org.apache.myrmidon.framework.nativelib.ArgumentList;
import org.apache.myrmidon.framework.nativelib.Execute;

/**
 * Task to perform GET commands to Microsoft Visual Source Safe. <p>
 *
 * The following attributes are interpretted:
 * <tableborder="1">
 *
 *   <tr>
 *
 *     <th>
 *       Attribute
 *     </th>
 *
 *     <th>
 *       Values
 *     </th>
 *
 *     <th>
 *       Required
 *     </th>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       login
 *     </td>
 *
 *     <td>
 *       username,password
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       vsspath
 *     </td>
 *
 *     <td>
 *       SourceSafe path
 *     </td>
 *
 *     <td>
 *       Yes
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       localpath
 *     </td>
 *
 *     <td>
 *       Override the working directory and get to the specified path
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       writable
 *     </td>
 *
 *     <td>
 *       true or false
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       recursive
 *     </td>
 *
 *     <td>
 *       true or false
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       version
 *     </td>
 *
 *     <td>
 *       a version number to get
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       date
 *     </td>
 *
 *     <td>
 *       a date stamp to get at
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       label
 *     </td>
 *
 *     <td>
 *       a label to get for
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       quiet
 *     </td>
 *
 *     <td>
 *       suppress output (off by default)
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       autoresponse
 *     </td>
 *
 *     <td>
 *       What to respond with (sets the -I option). By default, -I- is used;
 *       values of Y or N will be appended to this.
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 * </table>
 * <p>
 *
 * Note that only one of version, date or label should be specified</p>
 *
 * @author Craig Cottingham
 * @author Andrew Everitt
 */
public class MSVSSGET extends MSVSS
{

    private File m_LocalPath = null;
    private boolean m_Recursive = false;
    private boolean m_Writable = false;
    private String m_Version = null;
    private String m_Date = null;
    private String m_Label = null;
    private String m_AutoResponse = null;
    private boolean m_Quiet = false;

    /**
     * Sets/clears quiet mode
     *
     * @param quiet The new Quiet value
     */
    public final void setQuiet( boolean quiet )
    {
        this.m_Quiet = quiet;
    }

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
     * Set the stored date string <p>
     *
     * Note we assume that if the supplied string has the value "null" that
     * something went wrong and that the string value got populated from a null
     * object. This happens if a ant variable is used e.g. date="${date}" when
     * date has not been defined to ant!
     *
     * @param date The new Date value
     */
    public void setDate( String date )
    {
        if( date.equals( "" ) || date.equals( "null" ) )
        {
            m_Date = null;
        }
        else
        {
            m_Date = date;
        }
    }

    /**
     * Set the labeled version to operate on in SourceSafe <p>
     *
     * Note we assume that if the supplied string has the value "null" that
     * something went wrong and that the string value got populated from a null
     * object. This happens if a ant variable is used e.g.
     * label="${label_server}" when label_server has not been defined to ant!
     *
     * @param label The new Label value
     */
    public void setLabel( String label )
    {
        if( label.equals( "" ) || label.equals( "null" ) )
        {
            m_Label = null;
        }
        else
        {
            m_Label = label;
        }
    }

    /**
     * Set the local path.
     *
     * @param localPath The new Localpath value
     */
    public void setLocalpath( final File localPath )
    {
        m_LocalPath = localPath;
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
     * Set the stored version string <p>
     *
     * Note we assume that if the supplied string has the value "null" that
     * something went wrong and that the string value got populated from a null
     * object. This happens if a ant variable is used e.g.
     * version="${ver_server}" when ver_server has not been defined to ant!
     *
     * @param version The new Version value
     */
    public void setVersion( String version )
    {
        if( version.equals( "" ) || version.equals( "null" ) )
        {
            m_Version = null;
        }
        else
        {
            m_Version = version;
        }
    }

    /**
     * Checks the value set for the autoResponse. if it equals "Y" then we
     * return -I-Y if it equals "N" then we return -I-N otherwise we return -I
     *
     * @param cmd Description of Parameter
     */
    public void getAutoresponse( ArgumentList cmd )
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
     * Builds and returns the -GL flag command if required <p>
     *
     * The localpath is created if it didn't exist
     *
     * @param cmd Description of Parameter
     */
    public void getLocalpathCommand( ArgumentList cmd )
        throws TaskException
    {
        if( m_LocalPath == null )
        {
            return;
        }
        else
        {
            // make sure m_LocalDir exists, create it if it doesn't
            if( !m_LocalPath.exists() )
            {
                boolean done = m_LocalPath.mkdirs();
                if( done == false )
                {
                    String msg = "Directory " + m_LocalPath + " creation was not " +
                        "successful for an unknown reason";
                    throw new TaskException( msg );
                }
                getContext().info( "Created dir: " + m_LocalPath.getAbsolutePath() );
            }

            cmd.addArgument( FLAG_OVERRIDE_WORKING_DIR + m_LocalPath );
        }
    }

    public void getQuietCommand( ArgumentList cmd )
    {
        if( m_Quiet )
        {
            cmd.addArgument( FLAG_QUIET );
        }
    }

    /**
     * @param cmd Description of Parameter
     */
    public void getRecursiveCommand( ArgumentList cmd )
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
     * Simple order of priority. Returns the first specified of version, date,
     * label If none of these was specified returns ""
     *
     * @param cmd Description of Parameter
     */
    public void getVersionCommand( ArgumentList cmd )
    {

        if( m_Version != null )
        {
            cmd.addArgument( FLAG_VERSION + m_Version );
        }
        else if( m_Date != null )
        {
            cmd.addArgument( FLAG_VERSION_DATE + m_Date );
        }
        else if( m_Label != null )
        {
            cmd.addArgument( FLAG_VERSION_LABEL + m_Label );
        }
    }

    /**
     * @param cmd Description of Parameter
     */
    public void getWritableCommand( ArgumentList cmd )
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
        Execute exe = new Execute();

        // first off, make sure that we've got a command and a vssdir ...
        if( getVsspath() == null )
        {
            String msg = "vsspath attribute must be set!";
            throw new TaskException( msg );
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss Get VSS items [-G] [-H] [-I-] [-N] [-O] [-R] [-V] [-W] [-Y] [-?]
        // as specified in the SS.EXE help
        exe.setExecutable( getSSCommand() );
        exe.addArgument( COMMAND_GET );

        // VSS items
        exe.addArgument( getVsspath() );
        // -GL
        getLocalpathCommand( exe );
        // -I- or -I-Y or -I-N
        getAutoresponse( exe );
        // -O-
        getQuietCommand( exe );
        // -R
        getRecursiveCommand( exe );
        // -V
        getVersionCommand( exe );
        // -W
        getWritableCommand( exe );
        // -Y
        getLoginCommand( exe );

        run( exe );
    }

}

