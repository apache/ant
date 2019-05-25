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
 * Performs ClearCase checkout.
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
 *   <tr>
 *      <td>notco</td>
 *      <td>Fail if it's already checked out to the current view. Set to false to ignore it.</td>
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
public class CCCheckout extends ClearCase {
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

    private boolean mReserved = true;
    private String mOut = null;
    private boolean mNdata = false;
    private String mBranch = null;
    private boolean mVersion = false;
    private boolean mNwarn = false;
    private String mComment = null;
    private String mCfile = null;
    private boolean mNotco = true;

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
        // cleartool checkout [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_CHECKOUT);

        checkOptions(commandLine);
        /*
         * If configured to not care about whether the element is
         * already checked out to the current view.
         * Then check to see if it is checked out.
         */
        if (!getNotco() && lsCheckout()) {
            getProject().log("Already checked out in this view: "
                    + getViewPathBasename(), Project.MSG_VERBOSE);
            return;
        }
        if (!getFailOnErr()) {
            getProject().log("Ignoring any errors that occur for: "
                    + getViewPathBasename(), Project.MSG_VERBOSE);
        }
        int result = run(commandLine);
        if (Execute.isFailure(result) && getFailOnErr()) {
            throw new BuildException("Failed executing: " + commandLine,
                getLocation());
        }
    }

    /**
     * Check to see if the element is checked out in the current view.
     */
    private boolean lsCheckout() {
        Commandline cmdl = new Commandline();

        // build the command line from what we got the format is
        // cleartool lsco [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        cmdl.setExecutable(getClearToolCommand());
        cmdl.createArgument().setValue(COMMAND_LSCO);
        cmdl.createArgument().setValue("-cview");
        cmdl.createArgument().setValue("-short");
        cmdl.createArgument().setValue("-d");
        // viewpath
        cmdl.createArgument().setValue(getViewPath());

        String result = runS(cmdl, getFailOnErr());

        return result != null && !result.isEmpty();
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
        } else if (getNoData()) {
            // -ndata
            cmd.createArgument().setValue(FLAG_NODATA);
        }

        if (getBranch() != null) {
            // -branch
            getBranchCommand(cmd);
        } else if (getVersion()) {
            // -version
            cmd.createArgument().setValue(FLAG_VERSION);
        }

        if (getNoWarn()) {
            // -nwarn
            cmd.createArgument().setValue(FLAG_NOWARN);
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

        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }

    /**
     * If true, checks out the file as reserved.
     *
     * @param reserved the status to set the flag to
     */
    public void setReserved(boolean reserved) {
        mReserved = reserved;
    }

    /**
     * Get reserved flag status
     *
     * @return boolean containing status of reserved flag
     */
    public boolean getReserved() {
        return mReserved;
    }

    /**
     * If true, checkout fails if the element is already checked out to the current view.
     *
     * @param notco the status to set the flag to
     * @since ant 1.6.1
     */
    public void setNotco(boolean notco) {
        mNotco = notco;
    }

    /**
     * Get notco flag status
     *
     * @return boolean containing status of notco flag
     * @since ant 1.6.1
     */
    public boolean getNotco() {
        return mNotco;
    }

    /**
     * Creates a writable file under a different filename.
     *
     * @param outf the path to the out file
     */
    public void setOut(String outf) {
        mOut = outf;
    }

    /**
     * Get out file
     *
     * @return String containing the path to the out file
     */
    public String getOut() {
        return mOut;
    }

    /**
     * If true, checks out the file but does not create an
     * editable file containing its data.
     *
     * @param ndata the status to set the flag to
     */
    public void setNoData(boolean ndata) {
        mNdata = ndata;
    }

    /**
     * Get nodata flag status
     *
     * @return boolean containing status of ndata flag
     */
    public boolean getNoData() {
        return mNdata;
    }

    /**
     * Specify a branch to check out the file to.
     *
     * @param branch the name of the branch
     */
    public void setBranch(String branch) {
        mBranch = branch;
    }

    /**
     * Get branch name
     *
     * @return String containing the name of the branch
     */
    public String getBranch() {
        return mBranch;
    }

    /**
     * If true, allows checkout of a version other than main latest.
     *
     * @param version the status to set the flag to
     */
    public void setVersion(boolean version) {
        mVersion = version;
    }

    /**
     * Get version flag status
     *
     * @return boolean containing status of version flag
     */
    public boolean getVersion() {
        return mVersion;
    }

    /**
     * If true, warning messages are suppressed.
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
     * Sets the comment string.
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
     * Specifies a file containing a comment.
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
     * Get the 'out' command
     *
     * @param cmd containing the command line string with or
     *                    without the out flag and path appended
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
     * @param cmd containing the command line string with or
                          without the branch flag and name appended
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
     * Get the 'cfile' command
     *
     * @param cmd containing the command line string with or
     *                    without the cfile flag and file appended
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

