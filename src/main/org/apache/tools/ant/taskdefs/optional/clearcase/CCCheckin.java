/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;




/**
 * Performs ClearCase checkin.
 *
 * <p>
 * The following attributes are interpreted:
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
 *      <td>comment</td>
 *      <td>Specify a comment. Only one of comment or cfile may be used.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>commentfile</td>
 *      <td>Specify a file containing a comment. Only one of comment or cfile may be used.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>nowarn</td>
 *      <td>Suppress warning messages</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>preservetime</td>
 *      <td>Preserve the modification time</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>keepcopy</td>
 *      <td>Keeps a copy of the file with a .keep extension</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>identical</td>
 *      <td>Allows the file to be checked in even if it is identical to the original</td>
 *      <td>No</td>
 *   <tr>
 * </table>
 *
 * @author Curtis White
 */
public class CCCheckin extends ClearCase {
    private String m_Comment = null;
    private String m_Cfile = null;
    private boolean m_Nwarn = false;
    private boolean m_Ptime = false;
    private boolean m_Keep = false;
    private boolean m_Identical = true;

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

        // build the command line from what we got. the format is
        // cleartool checkin [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_CHECKIN);

        checkOptions(commandLine);

        result = run(commandLine);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }


    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
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

        if (getNoWarn()) {
            // -nwarn
            cmd.createArgument().setValue(FLAG_NOWARN);
        }

        if (getPreserveTime()) {
            // -ptime
            cmd.createArgument().setValue(FLAG_PRESERVETIME);
        }

        if (getKeepCopy()) {
            // -keep
            cmd.createArgument().setValue(FLAG_KEEPCOPY);
        }

        if (getIdentical()) {
            // -identical
            cmd.createArgument().setValue(FLAG_IDENTICAL);
        }

        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }


    /**
     * Sets the comment string.
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
     * Specifies a file containing a comment.
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
     * If true, suppress warning messages.
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
     * If true, preserve the modification time.
     *
     * @param ptime the status to set the flag to
     */
    public void setPreserveTime(boolean ptime) {
        m_Ptime = ptime;
    }

    /**
     * Get preservetime flag status
     *
     * @return boolean containing status of preservetime flag
     */
    public boolean getPreserveTime() {
        return m_Ptime;
    }

    /**
     * If true, keeps a copy of the file with a .keep extension.
     *
     * @param keep the status to set the flag to
     */
    public void setKeepCopy(boolean keep) {
        m_Keep = keep;
    }

    /**
     * Get keepcopy flag status
     *
     * @return boolean containing status of keepcopy flag
     */
    public boolean getKeepCopy() {
        return m_Keep;
    }

    /**
     * If true, allows the file to be checked in even
     * if it is identical to the original.
     *
     * @param identical the status to set the flag to
     */
    public void setIdentical(boolean identical) {
        m_Identical = identical;
    }

    /**
     * Get identical flag status
     *
     * @return boolean containing status of identical flag
     */
    public boolean getIdentical() {
        return m_Identical;
    }


    /**
     * Get the 'comment' command
     *
     * @param cmd containing the command line string with or without the comment flag and string appended
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
     * Get the 'commentfile' command
     *
     * @param cmd containing the command line string with or without the commentfile flag and file appended
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
        /**
     * -nwarn flag -- suppresses warning messages
     */
    public static final String FLAG_NOWARN = "-nwarn";
        /**
     * -ptime flag -- preserves the modification time
     */
    public static final String FLAG_PRESERVETIME = "-ptime";
        /**
     * -keep flag -- keeps a copy of the file with a .keep extension
     */
    public static final String FLAG_KEEPCOPY = "-keep";
        /**
     * -identical flag -- allows the file to be checked in even if it is identical to the original
     */
    public static final String FLAG_IDENTICAL = "-identical";

}

