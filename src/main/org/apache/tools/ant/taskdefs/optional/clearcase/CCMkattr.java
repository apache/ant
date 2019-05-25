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
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;

/**
 * Task to perform mkattr command to ClearCase.
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
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>replace</td>
 *      <td>Replace the value of the attribute if it already exists</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>recurse</td>
 *      <td>Process each subdirectory under viewpath</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>version</td>
 *      <td>Identify a specific version to attach the attribute to</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>typename</td>
 *      <td>Name of the attribute type</td>
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>typevalue</td>
 *      <td>Value to attach to the attribute type</td>
 *      <td>Yes</td>
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
 *      <td>failonerr</td>
 *      <td>Throw an exception if the command fails. Default is true</td>
 *      <td>No</td>
 *   <tr>
 * </table>
 *
 */
public class CCMkattr extends ClearCase {
    /**
     * -replace flag -- replace the existing value of the attribute
     */
    public static final String FLAG_REPLACE = "-replace";
    /**
     * -recurse flag -- process all subdirectories
     */
    public static final String FLAG_RECURSE = "-recurse";
    /**
     * -version flag -- attach attribute to specified version
     */
    public static final String FLAG_VERSION = "-version";
    /**
     * -c flag -- comment to attach to the element
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

    private boolean mReplace = false;
    private boolean mRecurse = false;
    private String mVersion = null;
    private String mTypeName = null;
    private String mTypeValue = null;
    private String mComment = null;
    private String mCfile = null;

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

        // Check for required attributes
        if (getTypeName() == null) {
            throw new BuildException("Required attribute TypeName not specified");
        }
        if (getTypeValue() == null) {
            throw new BuildException("Required attribute TypeValue not specified");
        }
        // Default the viewpath to basedir if it is not specified
        if (getViewPath() == null) {
            setViewPath(aProj.getBaseDir().getPath());
        }

        // build the command line from what we got. the format is
        // cleartool mkattr [options...] [viewpath ...]
        // as specified in the CLEARTOOL help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_MKATTR);

        checkOptions(commandLine);

        if (!getFailOnErr()) {
            getProject().log("Ignoring any errors that occur for: "
                    + getViewPathBasename(), Project.MSG_VERBOSE);
        }

        // For debugging
        // System.out.println(commandLine.toString());

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
        } else if (getCommentFile() != null) {
            // -cfile
            getCommentFileCommand(cmd);
        } else {
            cmd.createArgument().setValue(FLAG_NOCOMMENT);
        }

        if (getTypeName() != null) {
            // type
            getTypeCommand(cmd);
        }
        if (getTypeValue() != null) {
            // type value
            getTypeValueCommand(cmd);
        }
        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }

    /**
     * Set the replace flag
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
     * Set recurse flag
     *
     * @param recurse the status to set the flag to
     */
    public void setRecurse(boolean recurse) {
        mRecurse = recurse;
    }

    /**
     * Get recurse flag status
     *
     * @return boolean containing status of recurse flag
     */
    public boolean getRecurse() {
        return mRecurse;
    }

    /**
     * Set the version flag
     *
     * @param version the status to set the flag to
     */
    public void setVersion(String version) {
        mVersion = version;
    }

    /**
     * Get version flag status
     *
     * @return boolean containing status of version flag
     */
    public String getVersion() {
        return mVersion;
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
     * Set the attribute type-name
     *
     * @param tn the type name
     */
    public void setTypeName(String tn) {
        mTypeName = tn;
    }

    /**
     * Get attribute type-name
     *
     * @return String containing type name
     */
    public String getTypeName() {
        return mTypeName;
    }

    /**
     * Set the attribute type-value
     *
     * @param tv the type value
     */
    public void setTypeValue(String tv) {
        mTypeValue = tv;
    }

    /**
     * Get the attribute type-value
     *
     * @return String containing type value
     */
    public String getTypeValue() {
        return mTypeValue;
    }

    /**
     * Get the 'version' command
     *
     * @param cmd CommandLine containing the command line string with or
     *                    without the version flag and string appended
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
     * @param cmd         containing the command line string with or
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

    /**
     * Get the attribute type-name
     *
     * @param cmd containing the command line string with or
     *        without the type-name
     */
    private void getTypeCommand(Commandline cmd) {
        String typenm = getTypeName();

        if (typenm != null) {
            cmd.createArgument().setValue(typenm);
        }
    }

    /**
     * Get the attribute type-value
     *
     * @param cmd containing the command line string with or
     *        without the type-value
     */
    private void getTypeValueCommand(Commandline cmd) {
        String typevl = getTypeValue();

        if (typevl != null) {
            if (Os.isFamily("windows")) {
                typevl = "\\\"" + typevl + "\\\""; // Windows quoting of the value
            } else {
                typevl = "\"" + typevl + "\"";
            }
            cmd.createArgument().setValue(typevl);
        }
    }

}
