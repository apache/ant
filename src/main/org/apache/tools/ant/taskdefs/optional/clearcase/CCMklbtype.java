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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;

/**
 * Task to perform mklbtype command to ClearCase.
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
 *      <td>typename</td>
 *      <td>Name of the label type to create</td>
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>vob</td>
 *      <td>Name of the VOB</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>replace</td>
 *      <td>Replace an existing label definition of the same type</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>global</td>
 *      <td>Either global or ordinary can be specified, not both.
 *          Creates a label type that is global to the VOB or to
 *          VOBs that use this VOB</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>ordinary</td>
 *      <td>Either global or ordinary can be specified, not both.
 *          Creates a label type that can be used only in the current
 *          VOB. <B>Default</B></td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>pbranch</td>
 *      <td>Allows the label type to be used once per branch in a given
 *          element's version tree</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>shared</td>
 *      <td>Sets the way mastership is checked by ClearCase. See ClearCase
 *          documentation for details</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>comment</td>
 *      <td>Specify a comment. Only one of comment or cfile may be used.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>commentfile</td>
 *      <td>Specify a file containing a comment. Only one of comment or
 *          cfile may be used.</td>
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
public class CCMklbtype extends ClearCase {
    /**
     * -replace flag -- replace existing label definition of the same type
     */
    public static final String FLAG_REPLACE = "-replace";
    /**
     * -global flag -- creates a label type that is global to the VOB or to VOBs that use this VOB
     */
    public static final String FLAG_GLOBAL = "-global";
    /**
     * -ordinary flag -- creates a label type that can be used only in the current VOB
     */
    public static final String FLAG_ORDINARY = "-ordinary";
    /**
     * -pbranch flag -- allows label type to be used once per branch
     */
    public static final String FLAG_PBRANCH = "-pbranch";
    /**
     * -shared flag -- sets the way mastership is checked by ClearCase
     */
    public static final String FLAG_SHARED = "-shared";
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

    private String mTypeName = null;
    private String mVOB = null;
    private String mComment = null;
    private String mCfile = null;
    private boolean mReplace = false;
    private boolean mGlobal = false;
    private boolean mOrdinary = true;
    private boolean mPbranch = false;
    private boolean mShared = false;

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

        // Check for required attributes
        if (getTypeName() == null) {
            throw new BuildException("Required attribute TypeName not specified");
        }

        // build the command line from what we got. the format is
        // cleartool mklbtype [options...] type-selector...
        // as specified in the CLEARTOOL help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_MKLBTYPE);

        checkOptions(commandLine);

        if (!getFailOnErr()) {
            getProject().log("Ignoring any errors that occur for: "
                    + getTypeSpecifier(), Project.MSG_VERBOSE);
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
        if (getReplace()) {
            // -replace
            cmd.createArgument().setValue(FLAG_REPLACE);
        }

        if (getOrdinary()) {
            // -ordinary
            cmd.createArgument().setValue(FLAG_ORDINARY);
        } else if (getGlobal()) {
            // -global
            cmd.createArgument().setValue(FLAG_GLOBAL);
        }

        if (getPbranch()) {
            // -pbranch
            cmd.createArgument().setValue(FLAG_PBRANCH);
        }

        if (getShared()) {
            // -shared
            cmd.createArgument().setValue(FLAG_SHARED);
        }

        if (getComment() != null) {
            // -c
            getCommentCommand(cmd);
        } else if (getCommentFile() != null) {
            // -cfile
            getCommentFileCommand(cmd);
        } else {
            cmd.createArgument().setValue(FLAG_NOCOMMENT);
        }

        // type-name@vob
        cmd.createArgument().setValue(getTypeSpecifier());
    }

    /**
     * Set type-name string
     *
     * @param tn the type-name string
     */
    public void setTypeName(String tn) {
        mTypeName = tn;
    }

    /**
     * Get type-name string
     *
     * @return String containing the type-name
     */
    public String getTypeName() {
        return mTypeName;
    }

    /**
     * Set the VOB name
     *
     * @param vob the VOB name
     */
    public void setVOB(String vob) {
        mVOB = vob;
    }

    /**
     * Get VOB name
     *
     * @return String containing VOB name
     */
    public String getVOB() {
        return mVOB;
    }

    /**
     * Set the replace flag
     *
     * @param repl the status to set the flag to
     */
    public void setReplace(boolean repl) {
        mReplace = repl;
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
     * Set the global flag
     *
     * @param glob the status to set the flag to
     */
    public void setGlobal(boolean glob) {
        mGlobal = glob;
    }

    /**
     * Get global flag status
     *
     * @return boolean containing status of global flag
     */
    public boolean getGlobal() {
        return mGlobal;
    }

    /**
     * Set the ordinary flag
     *
     * @param ordinary the status to set the flag to
     */
    public void setOrdinary(boolean ordinary) {
        mOrdinary = ordinary;
    }

    /**
     * Get ordinary flag status
     *
     * @return boolean containing status of ordinary flag
     */
    public boolean getOrdinary() {
        return mOrdinary;
    }

    /**
     * Set the pbranch flag
     *
     * @param pbranch the status to set the flag to
     */
    public void setPbranch(boolean pbranch) {
        mPbranch = pbranch;
    }

    /**
     * Get pbranch flag status
     *
     * @return boolean containing status of pbranch flag
     */
    public boolean getPbranch() {
        return mPbranch;
    }

    /**
     * Set the shared flag
     *
     * @param shared the status to set the flag to
     */
    public void setShared(boolean shared) {
        mShared = shared;
    }

    /**
     * Get shared flag status
     *
     * @return boolean containing status of shared flag
     */
    public boolean getShared() {
        return mShared;
    }

    /**
     * Set comment string
     *
     * @param comment the comment string
     */
    public void setComment(String comment) {
        mComment = comment;
    }

    /**
     * Get comment string
     *
     * @return String containing the comment
     */
    public String getComment() {
        return mComment;
    }

    /**
     * Set comment file
     *
     * @param cfile the path to the comment file
     */
    public void setCommentFile(String cfile) {
        mCfile = cfile;
    }

    /**
     * Get comment file
     *
     * @return String containing the path to the comment file
     */
    public String getCommentFile() {
        return mCfile;
    }

    /**
     * Get the 'comment' command
     *
     * @param cmd containing the command line string with or
     *        without the comment flag and string appended
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
     * @param cmd containing the command line string with or
     *        without the commentfile flag and file appended
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
     * Get the type-name specifier
     *
     * @return the 'type-name-specifier' command if the attribute was
     *         specified, otherwise an empty string
     */
    private String getTypeSpecifier() {
        String typenm = getTypeName();
        if (getVOB() != null) {
            typenm += "@" + getVOB();
        }
        return typenm;
    }

}
