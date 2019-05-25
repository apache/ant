/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.clearcase;

import java.util.Optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;


/*
 * TODO:
 * comment field doesn't include all options yet
 */

/**
 * Performs a ClearCase Lock command.
 *
 * <p>
 * The following attributes are interpreted:
 * <table border="1">
 *   <caption>Task attributes</caption>
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
 *      <td>Specifies user(s) who can still modify the object/pname</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>obsolete</td>
 *      <td>Specifies that the object/pname should be marked obsolete</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>comment</td>
 *      <td>Specifies how to populate comments fields</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>pname</td>
 *      <td>Specifies the pathname to be locked.</td>
 *      <td>No</td>
 *   <tr>
 *      <td>objselect</td>
 *      <td>This variable is obsolete. Should use <i>objsel</i> instead.</td>
 *      <td>No</td>
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
 * </table>
 *
 */
public class CCLock extends ClearCase {
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

    private boolean mReplace = false;
    private boolean mObsolete = false;
    private String mComment = null;
    private String mNusers = null;
    private String mPname = null;
    private String mObjselect = null;

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute cleartool and then calls Exec's run method
     * to execute the command line.
     * @throws BuildException if the command fails and failonerr is set to true
     */
    @Override
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        Project aProj = getProject();

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
        // System.out.println(commandLine.toString());

        if (!getFailOnErr()) {
            getProject().log("Ignoring any errors that occur for: "
                    + getOpType(), Project.MSG_VERBOSE);
        }
        int result = run(commandLine);
        if (Execute.isFailure(result) && getFailOnErr()) {
            throw new BuildException("Failed executing: " + commandLine,
                getLocation());
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

        if (getObjselect() == null && getPname() == null) {
            throw new BuildException(
                "Should select either an element (pname) or an object (objselect)");
        }
        getPnameCommand(cmd);
        // object selector
        if (getObjselect() != null) {
            cmd.createArgument().setValue(getObjselect());
        }
    }

    /**
     * If true, replace an existing lock.
     *
     * @param replace the status to set the flag to
     */
    public void setReplace(boolean replace) {
        mReplace = replace;
    }

    /**
     * Get replace flag status
     *
     * @return boolean containing status of replace flag
     */
    public boolean getReplace() {
        return mReplace;
    }

    /**
     * If true, mark object as obsolete.
     *
     * @param obsolete the status to set the flag to
     */
    public void setObsolete(boolean obsolete) {
        mObsolete = obsolete;
    }

    /**
     * Get obsolete flag status
     *
     * @return boolean containing status of obsolete flag
     */
    public boolean getObsolete() {
        return mObsolete;
    }

    /**
     * Sets the users who may continue to
     * edit the object while it is locked.
     *
     * @param nusers users excluded from lock
     */
    public void setNusers(String nusers) {
        mNusers = nusers;
    }

    /**
     * Get nusers list
     *
     * @return String containing the list of users excluded from lock
     */
    public String getNusers() {
        return mNusers;
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
     * @param objsel objects to be locked
     * @since ant 1.6.1
     */
    public void setObjSel(String objsel) {
        mObjselect = objsel;
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
     * Get list of objects to be locked
     *
     * @return String containing the objects to be locked
     */
    public String getObjselect() {
        return mObjselect;
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
        }
        /* Had to make two separate commands here because if a space is
           inserted between the flag and the value, it is treated as a
           Windows filename with a space and it is enclosed in double
           quotes ("). This breaks clearcase.
        */
        cmd.createArgument().setValue(FLAG_NUSERS);
        cmd.createArgument().setValue(getNusers());
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
        }
        /* Had to make two separate commands here because if a space is
           inserted between the flag and the value, it is treated as a
           Windows filename with a space and it is enclosed in double
           quotes ("). This breaks clearcase.
        */
        cmd.createArgument().setValue(FLAG_COMMENT);
        cmd.createArgument().setValue(getComment());
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
        }
        /* Had to make two separate commands here because if a space is
           inserted between the flag and the value, it is treated as a
           Windows filename with a space and it is enclosed in double
           quotes ("). This breaks clearcase.
        */
        cmd.createArgument().setValue(FLAG_PNAME);
        cmd.createArgument().setValue(getPname());
    }

    /**
     * Return which object/pname is being operated on
     *
     * @return String containing the object/pname being worked on
     */
    private String getOpType() {
        return Optional.ofNullable(getPname()).orElseGet(this::getObjselect);
    }

}
