/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;

/**
 * Task to perform LABEL commands to Microsoft Visual Source Safe. <p>
 *
 * The following attributes are interpreted:
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
 *       ssdir
 *     </td>
 *
 *     <td>
 *       directory where <code>ss.exe</code> resides. By default the task
 *       expects it to be in the PATH.
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
 *       A label to apply to the hierarchy
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
 *       version
 *     </td>
 *
 *     <td>
 *       An existing file or project version to label
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
 *   <tr>
 *
 *     <td>
 *       comment
 *     </td>
 *
 *     <td>
 *       The comment to use for this label. Empty or '-' for no comment.
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *   </tr>
 *
 * </table>
 *
 *
 * @author Phillip Wells
 */
public class MSVSSLABEL extends MSVSS
{

    public final static String FLAG_LABEL = "-L";
    private String m_AutoResponse = null;
    private String m_Label = null;
    private String m_Version = null;
    private String m_Comment = "-";

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
     * Set the label to apply in SourceSafe <p>
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
     * Gets the label to be applied.
     *
     * @return the label to be applied.
     */
    public String getLabel()
    {
        return m_Label;
    }

    /**
     * Builds the label command.
     *
     * @param cmd the commandline the command is to be added to
     */
    public void getLabelCommand( Commandline cmd )
    {
        if( m_Label != null )
        {
            cmd.addArgument( FLAG_LABEL + m_Label );
        }
    }

    /**
     * Builds the version command.
     *
     * @param cmd the commandline the command is to be added to
     */
    public void getVersionCommand( Commandline cmd )
    {
        if( m_Version != null )
        {
            cmd.addArgument( FLAG_VERSION + m_Version );
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

        // first off, make sure that we've got a command and a vssdir and a label ...
        if( getVsspath() == null )
        {
            String msg = "vsspath attribute must be set!";
            throw new TaskException( msg );
        }
        if( getLabel() == null )
        {
            String msg = "label attribute must be set!";
            throw new TaskException( msg );
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss Label VSS items [-C]      [-H] [-I-] [-Llabel] [-N] [-O]      [-V]      [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable( getSSCommand() );
        commandLine.addArgument( COMMAND_LABEL );

        // VSS items
        commandLine.addArgument( getVsspath() );

        // -C
        commandLine.addArgument( "-C" + getComment() );

        // -I- or -I-Y or -I-N
        getAutoresponse( commandLine );

        // -L
        // Specify the new label on the command line (instead of being prompted)
        getLabelCommand( commandLine );

        // -V
        // Label an existing file or project version
        getVersionCommand( commandLine );

        // -Y
        getLoginCommand( commandLine );

        result = run( commandLine );
        if( result != 0 )
        {
            String msg = "Failed executing: " + commandLine.toString();
            throw new TaskException( msg );
        }

    }
}
