/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.Commandline;

/**
 * Task to perform mklabel command to ClearCase.
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
 *      <td>replace</td>
 *      <td>Replace a label of the same type on the same branch</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>recurse</td>
 *      <td>Process each subdirectory under viewpath</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>version</td>
 *      <td>Identify a specific version to attach the label to</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>typename</td>
 *      <td>Name of the label type</td>
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>vob</td>
 *      <td>Name of the VOB</td>
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
public class CCMklabel extends ClearCase {
    private boolean m_Replace = false;
    private boolean m_Recurse = false;
    private String m_Version = null;
    private String m_TypeName = null;
    private String m_VOB = null;
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

        // Check for required attributes
        if (getTypeName() == null) {
            throw new BuildException("Required attribute TypeName not specified");
        }

        // Default the viewpath to basedir if it is not specified
        if (getViewPath() == null) {
            setViewPath(aProj.getBaseDir().getPath());
        }

        // build the command line from what we got. the format is
        // cleartool mklabel [options...] [viewpath ...]
        // as specified in the CLEARTOOL help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_MKLABEL);

        checkOptions(commandLine);

        result = run(commandLine);
        if ( result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }


    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
        if (getReplace()) {
            // -replace
            cmd.createArgument().setValue(FLAG_REPLACE);
        }

        if (getRecurse()) {
            // -recurse
            cmd.createArgument().setValue(FLAG_RECURSE);
        }

        if (getVersion() != null) {
            // -version
            getVersionCommand(cmd);
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

        if (getTypeName() != null) {
            // type
            getTypeCommand(cmd);
        }

        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }


    /**
     * Set the replace flag
     *
     * @param replace the status to set the flag to
     */
    public void setReplace(boolean repl) {
        m_Replace = repl;
    }

    /**
     * Get replace flag status
     *
     * @return boolean containing status of replace flag
     */
    public boolean getReplace() {
        return m_Replace;
    }

    /**
     * Set recurse flag
     *
     * @param recurse the status to set the flag to
     */
    public void setRecurse(boolean recurse) {
        m_Recurse = recurse;
    }

    /**
     * Get recurse flag status
     *
     * @return boolean containing status of recurse flag
     */
    public boolean getRecurse() {
        return m_Recurse;
    }

    /**
     * Set the version flag
     *
     * @param version the status to set the flag to
     */
    public void setVersion(String ver) {
        m_Version = ver;
    }

    /**
     * Get version flag status
     *
     * @return boolean containing status of version flag
     */
    public String getVersion() {
        return m_Version;
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
     * Set the type-name
     *
     * @param tn the type name
     */
    public void setTypeName(String tn) {
        m_TypeName = tn;
    }

    /**
     * Get type-name
     *
     * @return String containing type name
     */
    public String getTypeName() {
        return m_TypeName;
    }

    /**
     * Set the VOB name
     *
     * @param vob the VOB name
     */
    public void setVOB(String vob) {
        m_VOB = vob;
    }

    /**
     * Get VOB name
     *
     * @return String containing VOB name
     */
    public String getVOB() {
        return m_VOB;
    }

    /**
     * Get the 'version' command
     *
     * @return the 'version' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the version flag and string appended
     */
    private void getVersionCommand(Commandline cmd) {
        if (getVersion() != null) {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_VERSION);
            cmd.createArgument().setValue(getVersion());
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
     * Get the 'commentfile' command
     *
     * @return the 'commentfile' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the commentfile flag and file appended
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
     * Get the type-name
     *
     * @return the 'type-name-specifier' command if the attribute was specified, otherwise an empty string
     *
     * @param CommandLine containing the command line string with or without the type-name
     */
    private void getTypeCommand(Commandline cmd) {
        String typenm = null;

        if (getTypeName() != null) {
            typenm = getTypeName();
            if (getVOB() != null) {
                typenm += "@" + getVOB();
            }
            cmd.createArgument().setValue(typenm);
        }
    }


    /**
     * -replace flag -- replace another label of the same type
     */
    public static final String FLAG_REPLACE = "-replace";
    /**
     * -recurse flag -- process all subdirectories
     */
    public static final String FLAG_RECURSE = "-recurse";
    /**
     * -version flag -- attach label to specified version
     */
    public static final String FLAG_VERSION = "-version";
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

