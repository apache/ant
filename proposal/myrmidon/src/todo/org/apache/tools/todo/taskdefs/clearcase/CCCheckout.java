/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.clearcase;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.Commandline;

/**
 * Task to perform Checkout command to ClearCase. <p>
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
 *           reserved
 *         </td>
 *
 *         <td>
 *           Specifies whether to check out the file as reserved or not
 *         </td>
 *
 *         <td>
 *           Yes
 *         </td>
 *
 *         <tr>
 *
 *           <tr>
 *
 *             <td>
 *               out
 *             </td>
 *
 *             <td>
 *               Creates a writable file under a different filename
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
 *                   nodata
 *                 </td>
 *
 *                 <td>
 *                   Checks out the file but does not create an editable file
 *                   containing its data
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
 *                       branch
 *                     </td>
 *
 *                     <td>
 *                       Specify a branch to check out the file to
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
 *                           version
 *                         </td>
 *
 *                         <td>
 *                           Allows checkout of a version other than main latest
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
 *                               nowarn
 *                             </td>
 *
 *                             <td>
 *                               Suppress warning messages
 *                             </td>
 *
 *                             <td>
 *                               No
 *                             </td>
 *
 *                             <tr>
 *
 *                               <tr>
 *
 *                                 <td>
 *                                   comment
 *                                 </td>
 *
 *                                 <td>
 *                                   Specify a comment. Only one of comment or
 *                                   cfile may be used.
 *                                 </td>
 *
 *                                 <td>
 *                                   No
 *                                 </td>
 *
 *                                 <tr>
 *
 *                                   <tr>
 *
 *                                     <td>
 *                                       commentfile
 *                                     </td>
 *
 *                                     <td>
 *                                       Specify a file containing a comment.
 *                                       Only one of comment or cfile may be
 *                                       used.
 *                                     </td>
 *
 *                                     <td>
 *                                       No
 *                                     </td>
 *
 *                                     <tr>
 *
 *                                     </table>
 *
 *
 * @author Curtis White
 */
public class CCCheckout extends ClearCase
{

    /**
     * -reserved flag -- check out the file as reserved
     */
    public final static String FLAG_RESERVED = "-reserved";
    /**
     * -reserved flag -- check out the file as unreserved
     */
    public final static String FLAG_UNRESERVED = "-unreserved";
    /**
     * -out flag -- create a writable file under a different filename
     */
    public final static String FLAG_OUT = "-out";
    /**
     * -ndata flag -- checks out the file but does not create an editable file
     * containing its data
     */
    public final static String FLAG_NODATA = "-ndata";
    /**
     * -branch flag -- checks out the file on a specified branch
     */
    public final static String FLAG_BRANCH = "-branch";
    /**
     * -version flag -- allows checkout of a version that is not main latest
     */
    public final static String FLAG_VERSION = "-version";
    /**
     * -nwarn flag -- suppresses warning messages
     */
    public final static String FLAG_NOWARN = "-nwarn";
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
    private boolean m_Reserved = true;
    private String m_Out = null;
    private boolean m_Ndata = false;
    private String m_Branch = null;
    private boolean m_Version = false;
    private boolean m_Nwarn = false;
    private String m_Comment = null;
    private String m_Cfile = null;

    /**
     * Set branch name
     *
     * @param branch the name of the branch
     */
    public void setBranch( String branch )
    {
        m_Branch = branch;
    }

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
     * Set the nodata flag
     *
     * @param ndata the status to set the flag to
     */
    public void setNoData( boolean ndata )
    {
        m_Ndata = ndata;
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
     * Set out file
     *
     * @param outf the path to the out file
     */
    public void setOut( String outf )
    {
        m_Out = outf;
    }

    /**
     * Set reserved flag status
     *
     * @param reserved the status to set the flag to
     */
    public void setReserved( boolean reserved )
    {
        m_Reserved = reserved;
    }

    /**
     * Set the version flag
     *
     * @param version the status to set the flag to
     */
    public void setVersion( boolean version )
    {
        m_Version = version;
    }

    /**
     * Get branch name
     *
     * @return String containing the name of the branch
     */
    public String getBranch()
    {
        return m_Branch;
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
     * Get nodata flag status
     *
     * @return boolean containing status of ndata flag
     */
    public boolean getNoData()
    {
        return m_Ndata;
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
     * Get out file
     *
     * @return String containing the path to the out file
     */
    public String getOut()
    {
        return m_Out;
    }

    /**
     * Get reserved flag status
     *
     * @return boolean containing status of reserved flag
     */
    public boolean getReserved()
    {
        return m_Reserved;
    }

    /**
     * Get version flag status
     *
     * @return boolean containing status of version flag
     */
    public boolean getVersion()
    {
        return m_Version;
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

        // build the command line from what we got the format is
        // cleartool checkout [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable( getClearToolCommand() );
        commandLine.addArgument( COMMAND_CHECKOUT );

        checkOptions( commandLine );

        run( commandLine );
    }

    /**
     * Get the 'branch' command
     *
     * @param cmd Description of Parameter
     */
    private void getBranchCommand( Commandline cmd )
    {
        if( getBranch() != null )
        {
            /*
             * Had to make two separate commands here because if a space is
             * inserted between the flag and the value, it is treated as a
             * Windows filename with a space and it is enclosed in double
             * quotes ("). This breaks clearcase.
             */
            cmd.addArgument( FLAG_BRANCH );
            cmd.addArgument( getBranch() );
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
     * Get the 'cfile' command
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
     * Get the 'out' command
     *
     * @param cmd Description of Parameter
     */
    private void getOutCommand( Commandline cmd )
    {
        if( getOut() != null )
        {
            /*
             * Had to make two separate commands here because if a space is
             * inserted between the flag and the value, it is treated as a
             * Windows filename with a space and it is enclosed in double
             * quotes ("). This breaks clearcase.
             */
            cmd.addArgument( FLAG_OUT );
            cmd.addArgument( getOut() );
        }
    }

    /**
     * Check the command line options.
     *
     * @param cmd Description of Parameter
     */
    private void checkOptions( Commandline cmd )
    {
        // ClearCase items
        if( getReserved() )
        {
            // -reserved
            cmd.addArgument( FLAG_RESERVED );
        }
        else
        {
            // -unreserved
            cmd.addArgument( FLAG_UNRESERVED );
        }

        if( getOut() != null )
        {
            // -out
            getOutCommand( cmd );
        }
        else
        {
            if( getNoData() )
            {
                // -ndata
                cmd.addArgument( FLAG_NODATA );
            }

        }

        if( getBranch() != null )
        {
            // -branch
            getBranchCommand( cmd );
        }
        else
        {
            if( getVersion() )
            {
                // -version
                cmd.addArgument( FLAG_VERSION );
            }

        }

        if( getNoWarn() )
        {
            // -nwarn
            cmd.addArgument( FLAG_NOWARN );
        }

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

        // viewpath
        cmd.addArgument( getViewPath() );
    }

}

