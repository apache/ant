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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;


/**
 * TODO:
 * comment field doesn't include all options yet
 */



/**
 * Performs a ClearCase Lock command.
 *
 * <p>
 * The following attributes are interpretted:
 * <table border="1">
 *   <tr>
 *     <th>Attribute</th>
 *     <th>Values</th>
 *     <th>Required</th>
 *   </tr>
 *   <tr>
 *      <td>replace</td>
 *      <td>Specifies replacing an existing lock</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>nusers</td>
 *      <td>Specifies user(s) who can still modify the object</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>obsolete</td>
 *      <td>Specifies that the object should be marked obsolete</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>comment</td>
 *      <td>Specifies how to populate comments fields</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>pname</td>
 *      <td>Specifies the object pathname to be locked.</td>
 *      <td>Yes</td>
 *   <tr>
 *      <td>objselect</td>
 *      <td>Specifies the object(s) to be locked.</td>
 *      <td>Yes</td>
 *   <tr>
 *
 * </table>
 *
 * @author Sean P. Kane (Based on work by: Curtis White)
 */
public class CCLock extends ClearCase {
    private boolean m_Replace = false;
    private boolean m_Obsolete = false;
    private String m_Comment = null;
    private String m_Nusers = null;
    private String m_Pname = null;
    private String m_Objselect = null;

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
        // cleartool lock [options...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_LOCK);

        // Check the command line options
        checkOptions(commandLine);

        // For debugging
        System.out.println(commandLine.toString());

        result = run(commandLine);
        if (Execute.isFailure(result)) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }

    /**
     * Check the command line options.
     */
private void checkOptions(Commandline cmd) {
        // ClearCase items
        if (getReplace()) {
            // -replace
            cmd.createArgument().setValue(FLAG_REPLACE);
        }
        if (getObsolete()) {
            // -obsolete
            cmd.createArgument().setValue(FLAG_OBSOLETE);
        } else {
                       getNusersCommand(cmd);
        }
        getCommentCommand(cmd);
        getPnameCommand(cmd);
               // object selector
        cmd.createArgument().setValue(getObjselect());
}

    /**
     * If true, replace an existing lock.
     *
     * @param replace the status to set the flag to
     */
    public void setReplace(boolean replace) {
        m_Replace = replace;
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
     * If true, mark object as obsolete.
     *
     * @param obsolete the status to set the flag to
     */
    public void setObsolete(boolean obsolete) {
        m_Obsolete = obsolete;
    }

    /**
     * Get obsolete flag status
     *
     * @return boolean containing status of obsolete flag
     */
    public boolean getObsolete() {
        return m_Obsolete;
    }

    /**
     * Sets the users who may continue to
     * edit the object while it is locked.
     *
     * @param nusers users excluded from lock
     */
    public void setNusers(String nusers) {
        m_Nusers = nusers;
    }

    /**
     * Get nusers list
     *
     * @return String containing the list of users excluded from lock
     */
    public String getNusers() {
        return m_Nusers;
    }

    /**
     * Sets how comments should be written
     * for the event record(s)
     *
     * @param comment comment method to use
     */
    public void setComment(String comment) {
        m_Comment = comment;
    }

    /**
     * Get comment method
     *
     * @return String containing the desired comment method
     */
    public String getComment() {
        return m_Comment;
    }

    /**
     * Sets the pathname to be locked
     *
     * @param pname pathname to be locked
     */
    public void setPname(String pname) {
        m_Pname = pname;
    }

    /**
     * Get the pathname to be locked
     *
     * @return String containing the pathname to be locked
     */
    public String getPname() {
        return m_Pname;
    }

    /**
     * Sets the object(s) to be locked
     *
     * @param objselect objects to be locked
     */
    public void setObjselect(String objselect) {
        m_Objselect = objselect;
    }

    /**
     * Get list of objects to be locked
     *
     * @return String containing the objects to be locked
     */
    public String getObjselect() {
        return m_Objselect;
    }

    /**
     * Get the 'nusers' command
     *
     * @param cmd containing the command line string with or
     *            without the nusers flag and value appended
     */
    private void getNusersCommand(Commandline cmd) {
        if (getNusers() == null) {
            return;
        } else {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_NUSERS);
            cmd.createArgument().setValue(getNusers());
        }
    }

    /**
     * Get the 'comment' command
     *
     * @param cmd containing the command line string with or without the
     *            comment flag and value appended
     */
    private void getCommentCommand(Commandline cmd) {
        if (getComment() == null) {
            return;
        } else {
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
     * Get the 'pname' command
     *
     * @param cmd containing the command line string with or
     *            without the pname flag and value appended
     */
    private void getPnameCommand(Commandline cmd) {
        if (getPname() == null) {
            return;
        } else {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_PNAME);
            cmd.createArgument().setValue(getPname());
        }
    }

    /**
     * Get the 'pname' command
     *
     * @param cmd containing the command line string with or
     *            without the pname flag and value appended
     */
    private void getObjselectCommand(Commandline cmd) {
        if (getObjselect() == null) {
            return;
        } else {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_OBJSELECT);
            cmd.createArgument().setValue(getPname());
        }
    }

    /**
     *  -replace flag -- replace existing lock on object(s)
     */
    public static final String FLAG_REPLACE = "-replace";
    /**
     * -nusers flag -- list of users to exclude from lock
     */
    public static final String FLAG_NUSERS = "-nusers";
    /**
     * -obsolete flag -- mark locked object as obsolete
     */
    public static final String FLAG_OBSOLETE = "-obsolete";
    /**
     * -comment flag -- method to use for commenting events
     */
    public static final String FLAG_COMMENT = "-comment";
    /**
     * -pname flag -- pathname to lock
     */
    public static final String FLAG_PNAME = "-pname";
    /**
     * object-selector option -- list of objects to lock
     */
    public static final String FLAG_OBJSELECT = "";
}

