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
 * Task to CreateBaseline command to ClearCase.
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
 *      <td>comment</td>
 *      <td>Specify a comment. Only one of comment or cfile may be
used.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>commentfile</td>
 *      <td>Specify a file containing a comment. Only one of comment or
cfile may be used.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>baselinerootname</td>
 *      <td>Specify the name to be associated with the baseline.</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>nowarn</td>
 *      <td>Suppress warning messages</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>identical</td>
 *      <td>Allows the baseline to be created even if it is identical to the
previous baseline.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>full</td>
 *      <td>Creates a full baseline.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>nlabel</td>
 *      <td>Allows the baseline to be created without a label.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>failonerr</td>
 *      <td>Throw an exception if the command fails. Default is true</td>
 *      <td>No</td>
 *   <tr>
 * </table>
 *
 */
public class CCMkbl extends ClearCase {

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
     * -identical flag -- allows the file to be checked in even if it is identical to the original
     */
    public static final String FLAG_IDENTICAL = "-identical";
    /**
     * -incremental flag -- baseline to be created is incremental
     */
    public static final String FLAG_INCREMENTAL = "-incremental";
    /**
     * -full flag -- baseline to be created is full
     */
    public static final String FLAG_FULL = "-full";
    /**
     * -nlabel -- baseline to be created without a label
     */
    public static final String FLAG_NLABEL = "-nlabel";

    private String mComment = null;
    private String mCfile = null;
    private String mBaselineRootName = null;
    private boolean mNwarn = false;
    private boolean mIdentical = true;
    private boolean mFull = false;
    private boolean mNlabel = false;

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

        // build the command line from what we got. the format is
        // cleartool checkin [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_MKBL);

        checkOptions(commandLine);

        if (!getFailOnErr()) {
            getProject().log("Ignoring any errors that occur for: "
                    + getBaselineRootName(), Project.MSG_VERBOSE);
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
        if (getComment() != null) {
            // -c
            getCommentCommand(cmd);
        } else if (getCommentFile() != null) {
            // -cfile
            getCommentFileCommand(cmd);
        } else {
            cmd.createArgument().setValue(FLAG_NOCOMMENT);
        }

        if (getIdentical()) {
            // -identical
            cmd.createArgument().setValue(FLAG_IDENTICAL);
        }

       if (getFull()) {
           // -full
           cmd.createArgument().setValue(FLAG_FULL);
       } else {
           // -incremental
           cmd.createArgument().setValue(FLAG_INCREMENTAL);
       }

       if (getNlabel()) {
           // -nlabel
           cmd.createArgument().setValue(FLAG_NLABEL);
       }

       // baseline_root_name
        cmd.createArgument().setValue(getBaselineRootName());
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
     * Set baseline_root_name
     *
     * @param baselineRootName the name of the baseline
     */
    public void setBaselineRootName(String baselineRootName) {
        mBaselineRootName = baselineRootName;
    }

    /**
     * Get baseline_root_name
     *
     * @return String containing the name of the baseline
     */
    public String getBaselineRootName() {
        return mBaselineRootName;
    }

    /**
     * Set the nowarn flag
     *
     * @param nwarn the status to set the flag to
     */
    public void setNoWarn(boolean nwarn) {
        mNwarn = nwarn;
    }

    /**
     * Get nowarn flag status
     *
     * @return boolean containing status of nwarn flag
     */
    public boolean getNoWarn() {
        return mNwarn;
    }

    /**
     * Set the identical flag
     *
     * @param identical the status to set the flag to
     */
    public void setIdentical(boolean identical) {
        mIdentical = identical;
    }

    /**
     * Get identical flag status
     *
     * @return boolean containing status of identical flag
     */
    public boolean getIdentical() {
        return mIdentical;
    }

    /**
     * Set the full flag
     *
     * @param full the status to set the flag to
     */
    public void setFull(boolean full) {
        mFull = full;
    }

    /**
     * Get full flag status
     *
     * @return boolean containing status of full flag
     */
    public boolean getFull() {
        return mFull;
    }

    /**
     * Set the nlabel flag
     *
     * @param nlabel the status to set the flag to
     */
    public void setNlabel(boolean nlabel) {
        mNlabel = nlabel;
    }

    /**
     * Get nlabel status
     *
     * @return boolean containing status of nlabel flag
     */
    public boolean getNlabel() {
        return mNlabel;
    }

    /**
     * Get the 'comment' command
     *
     * @param cmd containing the command line string with or
     *                    without the comment flag and string appended
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
     * @param cmd CommandLine containing the command line string with or
     *                    without the commentfile flag and file appended
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

}
