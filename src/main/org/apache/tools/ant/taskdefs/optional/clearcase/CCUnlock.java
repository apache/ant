/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003-2004 The Apache Software Foundation.  All rights
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
 * Performs a ClearCase Unlock command.
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
 *      <td>comment</td>
 *      <td>Specifies how to populate comments fields</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>pname</td>
 *      <td>Specifies the object pathname to be unlocked.</td>
 *      <td>No</td>
 *   <tr>
 *      <td>objselect</td>
 *      <td>This variable is obsolete. Should use <i>objsel</i> instead.</td>
 *      <td>no</td>
 *   <tr>
 *   <tr>
 *      <td>objsel</td>
 *      <td>Specifies the object(s) to be unlocked.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>failonerr</td>
 *      <td>Throw an exception if the command fails. Default is true</td>
 *      <td>No</td>
 *   <tr>
 *
 * </table>
 *
 * @author Sean P. Kane (Based on work by: Curtis White)
 */
public class CCUnlock extends ClearCase {
    private String mComment = null;
    private String mPname = null;
    private String mObjselect = null;

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute cleartool and then calls Exec's run method
     * to execute the command line.
     * @throws BuildException if the command fails and failonerr is set to true
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
        commandLine.createArgument().setValue(COMMAND_UNLOCK);

        // Check the command line options
        checkOptions(commandLine);

        // For debugging
        // System.out.println(commandLine.toString());

        if (!getFailOnErr()) {
            getProject().log("Ignoring any errors that occur for: "
                    + getOpType(), Project.MSG_VERBOSE);
        }
        result = run(commandLine);
        if (Execute.isFailure(result) && getFailOnErr()) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, getLocation());
        }
    }

    /**
     * Check the command line options.
     */
private void checkOptions(Commandline cmd) {
        // ClearCase items
        getCommentCommand(cmd);

        if (getObjselect() == null && getPname() == null) {
            throw new BuildException("Should select either an element "
            + "(pname) or an object (objselect)");
        }
        getPnameCommand(cmd);
        // object selector
        if (getObjselect() != null) {
            cmd.createArgument().setValue(getObjselect());
        }
}

    /**
     * Sets how comments should be written
     * for the event record(s)
     *
     * @param comment comment method to use
     */
    public void setComment(String comment) {
        mComment = comment;
    }

    /**
     * Get comment method
     *
     * @return String containing the desired comment method
     */
    public String getComment() {
        return mComment;
    }

    /**
     * Sets the pathname to be locked
     *
     * @param pname pathname to be locked
     */
    public void setPname(String pname) {
        mPname = pname;
    }

    /**
     * Get the pathname to be locked
     *
     * @return String containing the pathname to be locked
     */
    public String getPname() {
        return mPname;
    }

    /**
     * Sets the object(s) to be locked
     *
     * @param objselect objects to be locked
     */
    public void setObjselect(String objselect) {
        mObjselect = objselect;
    }

    /**
     * Sets the object(s) to be locked
     *
     * @param objsel objects to be locked
     * @since ant 1.6.1
     */
    public void setObjSel(String objsel) {
        mObjselect = objsel;
    }

    /**
     * Get list of objects to be locked
     *
     * @return String containing the objects to be locked
     */
    public String getObjselect() {
        return mObjselect;
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
     * @param cmd containing the command line string with or without the
     *            pname flag and value appended
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
     * Return which object/pname is being operated on
     *
     * @return String containing the object/pname being worked on
     */
    private String getOpType() {

        if (getPname() != null) {
            return getPname();
        } else {
            return getObjselect();
        }
    }

    /**
     * -comment flag -- method to use for commenting events
     */
    public static final String FLAG_COMMENT = "-comment";
    /**
     * -pname flag -- pathname to lock
     */
    public static final String FLAG_PNAME = "-pname";
}

