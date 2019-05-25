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
 * Performs a ClearCase Update command.
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
 *      <td>graphical</td>
 *      <td>Displays a graphical dialog during the update</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>log</td>
 *      <td>Specifies a log file for ClearCase to write to</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>overwrite</td>
 *      <td>Specifies whether to overwrite hijacked files or not</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>rename</td>
 *      <td>Specifies that hijacked files should be renamed with a .keep extension</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>currenttime</td>
 *      <td>Specifies that modification time should be written as the current
 *          time. Either currenttime or preservetime can be specified.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>preservetime</td>
 *      <td>Specifies that modification time should preserved from the VOB
 *          time. Either currenttime or preservetime can be specified.</td>
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
public class CCUpdate extends ClearCase {
    /**
     *  -graphical flag -- display graphical dialog during update operation
     */
    public static final String FLAG_GRAPHICAL = "-graphical";
    /**
     * -log flag -- file to log status to
     */
    public static final String FLAG_LOG = "-log";
    /**
     * -overwrite flag -- overwrite hijacked files
     */
    public static final String FLAG_OVERWRITE = "-overwrite";
    /**
     * -noverwrite flag -- do not overwrite hijacked files
     */
    public static final String FLAG_NOVERWRITE = "-noverwrite";
    /**
     * -rename flag -- rename hijacked files with .keep extension
     */
    public static final String FLAG_RENAME = "-rename";
    /**
     * -ctime flag -- modified time is written as the current time
     */
    public static final String FLAG_CURRENTTIME = "-ctime";
    /**
     * -ptime flag -- modified time is written as the VOB time
     */
    public static final String FLAG_PRESERVETIME = "-ptime";

    private boolean mGraphical = false;
    private boolean mOverwrite = false;
    private boolean mRename = false;
    private boolean mCtime = false;
    private boolean mPtime = false;
    private String mLog = null;

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
        // cleartool update [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_UPDATE);

        // Check the command line options
        checkOptions(commandLine);

        // For debugging
        getProject().log(commandLine.toString(), Project.MSG_DEBUG);

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
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
        // ClearCase items
        if (getGraphical()) {
            // -graphical
            cmd.createArgument().setValue(FLAG_GRAPHICAL);
        } else {
            if (getOverwrite()) {
                // -overwrite
                cmd.createArgument().setValue(FLAG_OVERWRITE);
            } else if (getRename()) {
                // -rename
                cmd.createArgument().setValue(FLAG_RENAME);
            } else {
                // -noverwrite
                cmd.createArgument().setValue(FLAG_NOVERWRITE);
            }

            if (getCurrentTime()) {
                // -ctime
                cmd.createArgument().setValue(FLAG_CURRENTTIME);
            } else if (getPreserveTime()) {
                // -ptime
                cmd.createArgument().setValue(FLAG_PRESERVETIME);
            }

            // -log logname
            getLogCommand(cmd);
        }

        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }

    /**
     * If true, displays a graphical dialog during the update.
     *
     * @param graphical the status to set the flag to
     */
    public void setGraphical(boolean graphical) {
        mGraphical = graphical;
    }

    /**
     * Get graphical flag status
     *
     * @return boolean containing status of graphical flag
     */
    public boolean getGraphical() {
        return mGraphical;
    }

    /**
     * If true, overwrite hijacked files.
     *
     * @param ow the status to set the flag to
     */
    public void setOverwrite(boolean ow) {
        mOverwrite = ow;
    }

    /**
     * Get overwrite hijacked files status
     *
     * @return boolean containing status of overwrite flag
     */
    public boolean getOverwrite() {
        return mOverwrite;
    }

    /**
     * If true, hijacked files are renamed with a .keep extension.
     *
     * @param ren the status to set the flag to
     */
    public void setRename(boolean ren) {
        mRename = ren;
    }

    /**
     * Get rename hijacked files status
     *
     * @return boolean containing status of rename flag
     */
    public boolean getRename() {
        return mRename;
    }

    /**
     * If true, modification time should be written as the current time.
     * Either currenttime or preservetime can be specified.
     *
     * @param ct the status to set the flag to
     */
    public void setCurrentTime(boolean ct) {
        mCtime = ct;
    }

    /**
     * Get current time status
     *
     * @return boolean containing status of current time flag
     */
    public boolean getCurrentTime() {
        return mCtime;
    }

    /**
     * If true, modification time should be preserved from the VOB time.
     * Either currenttime or preservetime can be specified.
     *
     * @param pt the status to set the flag to
     */
    public void setPreserveTime(boolean pt) {
        mPtime = pt;
    }

    /**
     * Get preserve time status
     *
     * @return boolean containing status of preserve time flag
     */
    public boolean getPreserveTime() {
        return mPtime;
    }

    /**
     * Sets the log file where cleartool records
     * the status of the command.
     *
     * @param log the path to the log file
     */
    public void setLog(String log) {
        mLog = log;
    }

    /**
     * Get log file
     *
     * @return String containing the path to the log file
     */
    public String getLog() {
        return mLog;
    }

    /**
     * Get the 'log' command
     *
     * @param cmd containing the command line string with or without the log flag and path appended
     */
    private void getLogCommand(Commandline cmd) {
        if (getLog() == null) {
            return;
        }
        /* Had to make two separate commands here because if a space is
           inserted between the flag and the value, it is treated as a
           Windows filename with a space and it is enclosed in double
           quotes ("). This breaks clearcase.
        */
        cmd.createArgument().setValue(FLAG_LOG);
        cmd.createArgument().setValue(getLog());
    }

}
