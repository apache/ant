/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.clearcase;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;

/**
 * Task to perform Checkin command to ClearCase. <p>
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
 *       viewpath
 *     </td>
 *
 *     <td>
 *       Path to the ClearCase view file or directory that the command will
 *       operate on
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *     <tr>
 *
 *       <tr>
 *
 *         <td>
 *           comment
 *         </td>
 *
 *         <td>
 *           Specify a comment. Only one of comment or cfile may be used.
 *         </td>
 *
 *         <td>
 *           No
 *         </td>
 *
 *         <tr>
 *
 *           <tr>
 *
 *             <td>
 *               commentfile
 *             </td>
 *
 *             <td>
 *               Specify a file containing a comment. Only one of comment or
 *               cfile may be used.
 *             </td>
 *
 *             <td>
 *               No
 *             </td>
 *
 *             <tr>
 *
 *               <tr>
 *
 *                 <td>
 *                   nowarn
 *                 </td>
 *
 *                 <td>
 *                   Suppress warning messages
 *                 </td>
 *
 *                 <td>
 *                   No
 *                 </td>
 *
 *                 <tr>
 *
 *                   <tr>
 *
 *                     <td>
 *                       preservetime
 *                     </td>
 *
 *                     <td>
 *                       Preserve the modification time
 *                     </td>
 *
 *                     <td>
 *                       No
 *                     </td>
 *
 *                     <tr>
 *
 *                       <tr>
 *
 *                         <td>
 *                           keepcopy
 *                         </td>
 *
 *                         <td>
 *                           Keeps a copy of the file with a .keep extension
 *
 *                         </td>
 *
 *                         <td>
 *                           No
 *                         </td>
 *
 *                         <tr>
 *
 *                           <tr>
 *
 *                             <td>
 *                               identical
 *                             </td>
 *
 *                             <td>
 *                               Allows the file to be checked in even if it is
 *                               identical to the original
 *                             </td>
 *
 *                             <td>
 *                               No
 *                             </td>
 *
 *                             <tr>
 *
 *                             </table>
 *
 *
 * @author Curtis White
 */
public class CCCheckin extends ClearCase
{

    /**
     * -c flag -- comment to attach to the file
     */
    public final static String FLAG_COMMENT = "-c";
    /**
     * -cfile flag -- file containing a comment to attach to the file
     */
    public final static String FLAG_COMMENTFILE = "-cfile";
    /**
     * -nc flag -- no comment is specified
     */
    public final static String FLAG_NOCOMMENT = "-nc";
    /**
     * -nwarn flag -- suppresses warning messages
     */
    public final static String FLAG_NOWARN = "-nwarn";
    /**
     * -ptime flag -- preserves the modification time
     */
    public final static String FLAG_PRESERVETIME = "-ptime";
    /**
     * -keep flag -- keeps a copy of the file with a .keep extension
     */
    public final static String FLAG_KEEPCOPY = "-keep";
    /**
     * -identical flag -- allows the file to be checked in even if it is
     * identical to the original
     */
    public final static String FLAG_IDENTICAL = "-identical";
    private String m_Comment = null;
    private String m_Cfile = null;
    private boolean m_Nwarn = false;
    private boolean m_Ptime = false;
    private boolean m_Keep = false;
    private boolean m_Identical = true;

    /**
     * Set comment string
     *
     * @param comment the comment string
     */
    public void setComment( String comment )
    {
        m_Comment = comment;
    }

    /**
     * Set comment file
     *
     * @param cfile the path to the comment file
     */
    public void setCommentFile( String cfile )
    {
        m_Cfile = cfile;
    }

    /**
     * Set the identical flag
     *
     * @param identical the status to set the flag to
     */
    public void setIdentical( boolean identical )
    {
        m_Identical = identical;
    }

    /**
     * Set the keepcopy flag
     *
     * @param keep the status to set the flag to
     */
    public void setKeepCopy( boolean keep )
    {
        m_Keep = keep;
    }

    /**
     * Set the nowarn flag
     *
     * @param nwarn the status to set the flag to
     */
    public void setNoWarn( boolean nwarn )
    {
        m_Nwarn = nwarn;
    }

    /**
     * Set preservetime flag
     *
     * @param ptime the status to set the flag to
     */
    public void setPreserveTime( boolean ptime )
    {
        m_Ptime = ptime;
    }

    /**
     * Get comment string
     *
     * @return String containing the comment
     */
    public String getComment()
    {
        return m_Comment;
    }

    /**
     * Get comment file
     *
     * @return String containing the path to the comment file
     */
    public String getCommentFile()
    {
        return m_Cfile;
    }

    /**
     * Get identical flag status
     *
     * @return boolean containing status of identical flag
     */
    public boolean getIdentical()
    {
        return m_Identical;
    }

    /**
     * Get keepcopy flag status
     *
     * @return boolean containing status of keepcopy flag
     */
    public boolean getKeepCopy()
    {
        return m_Keep;
    }

    /**
     * Get nowarn flag status
     *
     * @return boolean containing status of nwarn flag
     */
    public boolean getNoWarn()
    {
        return m_Nwarn;
    }

    /**
     * Get preservetime flag status
     *
     * @return boolean containing status of preservetime flag
     */
    public boolean getPreserveTime()
    {
        return m_Ptime;
    }

    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute cleartool and then calls Exec's run
     * method to execute the command line.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        Commandline commandLine = new Commandline();

        // Default the viewpath to basedir if it is not specified
        if( getViewPath() == null )
        {
            setViewPath( getBaseDirectory().getPath() );
        }

        // build the command line from what we got. the format is
        // cleartool checkin [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable( getClearToolCommand() );
        commandLine.addArgument( COMMAND_CHECKIN );

        checkOptions( commandLine );

        final int result = run( commandLine );
        if( result != 0 )
        {
            final String message = "Failed executing: " + commandLine.toString();
            throw new TaskException( message );
        }
    }

    /**
     * Get the 'comment' command
     *
     * @param cmd Description of Parameter
     */
    private void getCommentCommand( Commandline cmd )
    {
        if( getComment() != null )
        {
            /*
             * Had to make two separate commands here because if a space is
             * inserted between the flag and the value, it is treated as a
             * Windows filename with a space and it is enclosed in double
             * quotes ("). This breaks clearcase.
             */
            cmd.addArgument( FLAG_COMMENT );
            cmd.addArgument( getComment() );
        }
    }

    /**
     * Get the 'commentfile' command
     *
     * @param cmd Description of Parameter
     */
    private void getCommentFileCommand( Commandline cmd )
    {
        if( getCommentFile() != null )
        {
            /*
             * Had to make two separate commands here because if a space is
             * inserted between the flag and the value, it is treated as a
             * Windows filename with a space and it is enclosed in double
             * quotes ("). This breaks clearcase.
             */
            cmd.addArgument( FLAG_COMMENTFILE );
            cmd.addArgument( getCommentFile() );
        }
    }

    /**
     * Check the command line options.
     *
     * @param cmd Description of Parameter
     */
    private void checkOptions( Commandline cmd )
    {
        if( getComment() != null )
        {
            // -c
            getCommentCommand( cmd );
        }
        else
        {
            if( getCommentFile() != null )
            {
                // -cfile
                getCommentFileCommand( cmd );
            }
            else
            {
                cmd.addArgument( FLAG_NOCOMMENT );
            }
        }

        if( getNoWarn() )
        {
            // -nwarn
            cmd.addArgument( FLAG_NOWARN );
        }

        if( getPreserveTime() )
        {
            // -ptime
            cmd.addArgument( FLAG_PRESERVETIME );
        }

        if( getKeepCopy() )
        {
            // -keep
            cmd.addArgument( FLAG_KEEPCOPY );
        }

        if( getIdentical() )
        {
            // -identical
            cmd.addArgument( FLAG_IDENTICAL );
        }

        // viewpath
        cmd.addArgument( getViewPath() );
    }

}

