/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.clearcase;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import java.io.File;

/**
 * Task to perform Checkout command to ClearCase.
 * <p>
 * The following attributes are interpretted:
 * <table border="1">
 *   <tr>
 *     <th>Attribute</th>
 *     <th>Values</th>
 *     <th>Required</th>
 *   </tr>
 *   <tr>
 *      <td>viewpath</td>
 *      <td>Path to the ClearCase view file or directory that the command will operate on</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>reserved</td>
 *      <td>Specifies whether to check out the file as reserved or not</td>
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>out</td>
 *      <td>Creates a writable file under a different filename</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>nodata</td>
 *      <td>Checks out the file but does not create an editable file containing its data</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>branch</td>
 *      <td>Specify a branch to check out the file to</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>version</td>
 *      <td>Allows checkout of a version other than main latest</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>nowarn</td>
 *      <td>Suppress warning messages</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>comment</td>
 *      <td>Specify a comment. Only one of comment or cfile may be used.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>commentfile</td>
 *      <td>Specify a file containing a comment. Only one of comment or cfile may be used.</td>
 *      <td>No</td>
 *   <tr>
 * </table>
 *
 * @author Curtis White
 */
public class CCCheckout extends ClearCase {
    private boolean m_Reserved = true;
    private String m_Out = null;
    private boolean m_Ndata = false;
    private String m_Branch = null;
    private boolean m_Version = false;
    private boolean m_Nwarn = false;
    private String m_Comment = null;
    private String m_Cfile = null;

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute cleartool and then calls Exec's run method
     * to execute the command line.
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        Project aProj = getProject();
        int result = 0;

        // Default the viewpath to basedir if it is not specified
        if (getViewPath() == null) {
            setViewPath(aProj.getBaseDir().getPath());
        }

        // build the command line from what we got the format is
        // cleartool checkout [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_CHECKOUT);

        checkOptions(commandLine);

        result = run(commandLine);
        if ( result != 0 ) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }


    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
        // ClearCase items
        if (getReserved()) {
            // -reserved
            cmd.createArgument().setValue(FLAG_RESERVED);
        } else {
            // -unreserved
            cmd.createArgument().setValue(FLAG_UNRESERVED);
        }

        if (getOut() != null) {
            // -out
            getOutCommand(cmd);
        } else {
            if (getNoData()) {
                // -ndata
                cmd.createArgument().setValue(FLAG_NODATA);
            }

        }

        if (getBranch() != null) {
            // -branch
            getBranchCommand(cmd);
        } else {
            if (getVersion()) {
                // -version
                cmd.createArgument().setValue(FLAG_VERSION);
            }

        }

        if (getNoWarn()) {
            // -nwarn
            cmd.createArgument().setValue(FLAG_NOWARN);
        }

        if (getComment() != null) {
            // -c
            getCommentCommand(cmd);
        } else {
            if (getCommentFile() != null) {
                // -cfile
                getCommentFileCommand(cmd);
            } else {
                cmd.createArgument().setValue(FLAG_NOCOMMENT);
            }
        }

        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }

    /**
     * Set reserved flag status
     *
     * @param reserved the status to set the flag to
     */
    public void setReserved(boolean reserved) {
        m_Reserved = reserved;
    }

    /**
     * Get reserved flag status
     *
     * @return boolean containing status of reserved flag
     */
    public boolean getReserved() {
        return m_Reserved;
    }

    /**
     * Set out file
     *
     * @param outf the path to the out file
     */
    public void setOut(String outf) {
        m_Out = outf;
    }

    /**
     * Get out file
     *
     * @return String containing the path to the out file
     */
    public String getOut() {
        return m_Out;
    }

    /**
     * Set the nodata flag
     *
     * @param ndata the status to set the flag to
     */
    public void setNoData(boolean ndata) {
        m_Ndata = ndata;
    }

    /**
     * Get nodata flag status
     *
     * @return boolean containing status of ndata flag
     */
    public boolean getNoData() {
        return m_Ndata;
    }

    /**
     * Set branch name
     *
     * @param branch the name of the branch
     */
    public void setBranch(String branch) {
        m_Branch = branch;
    }

    /**
     * Get branch name
     *
     * @return String containing the name of the branch
     */
    public String getBranch() {
        return m_Branch;
    }

    /**
     * Set the version flag
     *
     * @param version the status to set the flag to
     */
    public void setVersion(boolean version) {
        m_Version = version;
    }

    /**
     * Get version flag status
     *
     * @return boolean containing status of version flag
     */
    public boolean getVersion() {
        return m_Version;
    }

    /**
     * Set the nowarn flag
     *
     * @param nwarn the status to set the flag to
     */
    public void setNoWarn(boolean nwarn) {
        m_Nwarn = nwarn;
    }

    /**
     * Get nowarn flag status
     *
     * @return boolean containing status of nwarn flag
     */
    public boolean getNoWarn() {
        return m_Nwarn;
    }

    /**
     * Set comment string
     *
     * @param comment the comment string
     */
    public void setComment(String comment) {
        m_Comment = comment;
    }

    /**
     * Get comment string
     *
     * @return String containing the comment
     */
    public String getComment() {
        return m_Comment;
    }

    /**
     * Set comment file
     *
     * @param cfile the path to the comment file
     */
    public void setCommentFile(String cfile) {
        m_Cfile = cfile;
    }

    /**
     * Get comment file
     *
     * @return String containing the path to the comment file
     */
    public String getCommentFile() {
        return m_Cfile;
    }

    /**
     * Get the 'out' command
     *
     * @return the 'out' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the out flag and path appended
     */
    private void getOutCommand(Commandline cmd) {
        if (getOut() != null) {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_OUT);
            cmd.createArgument().setValue(getOut());
        }
    }

    /**
     * Get the 'branch' command
     *
     * @return the 'branch' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the branch flag and name appended
     */
    private void getBranchCommand(Commandline cmd) {
        if (getBranch() != null) {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_BRANCH);
            cmd.createArgument().setValue(getBranch());
        }
    }


    /**
     * Get the 'comment' command
     *
     * @return the 'comment' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the comment flag and string appended
     */
    private void getCommentCommand(Commandline cmd) {
        if (getComment() != null) {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_COMMENT);
            cmd.createArgument().setValue(getComment());
        }
    }

    /**
     * Get the 'cfile' command
     *
     * @return the 'cfile' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the cfile flag and file appended
     */
    private void getCommentFileCommand(Commandline cmd) {
        if (getCommentFile() != null) {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_COMMENTFILE);
            cmd.createArgument().setValue(getCommentFile());
        }
    }

        /**
     *  -reserved flag -- check out the file as reserved
     */
    public static final String FLAG_RESERVED = "-reserved";
        /**
     *  -reserved flag -- check out the file as unreserved
     */
    public static final String FLAG_UNRESERVED = "-unreserved";
        /**
     * -out flag -- create a writable file under a different filename
     */
    public static final String FLAG_OUT = "-out";
        /**
     * -ndata flag -- checks out the file but does not create an editable file containing its data
     */
    public static final String FLAG_NODATA = "-ndata";
        /**
     * -branch flag -- checks out the file on a specified branch
     */
    public static final String FLAG_BRANCH = "-branch";
        /**
     * -version flag -- allows checkout of a version that is not main latest
     */
    public static final String FLAG_VERSION = "-version";
        /**
     * -nwarn flag -- suppresses warning messages
     */
    public static final String FLAG_NOWARN = "-nwarn";
        /**
     * -c flag -- comment to attach to the file
     */
    public static final String FLAG_COMMENT = "-c";
        /**
     * -cfile flag -- file containing a comment to attach to the file
     */
    public static final String FLAG_COMMENTFILE = "-cfile";
        /**
     * -nc flag -- no comment is specified
     */
    public static final String FLAG_NOCOMMENT = "-nc";

}

