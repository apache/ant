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
 * Task to perform rmtype command to ClearCase.
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
 *      <td>typekind</td>
 *      <td>The kind of type to create. Valid types are:<br>
 *              attype                         attribute type<br>
 *              brtype                         branch type<br>
 *              eltype                         element type<br>
 *              hltype                         hyperlink type<br>
 *              lbtype                         label type<br>
 *              trtype                         trigger type
 *      </td>
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>typename</td>
 *      <td>The name of the type to remove</td>
 *      <td>Yes</td>
 *   <tr>
 *   <tr>
 *      <td>vob</td>
 *      <td>Name of the VOB</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>ignore</td>
 *      <td>Used with trigger types only. Forces removal of trigger type
 *          even if a pre-operation trigger would prevent its removal</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>rmall</td>
 *      <td>Removes all instances of a type and the type object itself</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>comment</td>
 *      <td>Specify a comment. Only one of comment or cfile may be used.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>commentfile</td>
 *      <td>Specify a file containing a comment. Only one of comment or cfile
 *          may be used.</td>
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
public class CCRmtype extends ClearCase {
    /**
     * -ignore flag -- ignore pre-trigger operations when removing a trigger type
     */
    public static final String FLAG_IGNORE = "-ignore";
    /**
     * -rmall flag -- removes all instances of a type and the type object itself
     */
    public static final String FLAG_RMALL = "-rmall";
    /**
     * -force flag -- suppresses confirmation prompts
     */
    public static final String FLAG_FORCE = "-force";
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

    private String mTypeKind = null;
    private String mTypeName = null;
    private String mVOB = null;
    private String mComment = null;
    private String mCfile = null;
    private boolean mRmall = false;
    private boolean mIgnore = false;

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
        if (getTypeKind() == null) {
            throw new BuildException("Required attribute TypeKind not specified");
        }
        if (getTypeName() == null) {
            throw new BuildException("Required attribute TypeName not specified");
        }

        // build the command line from what we got. the format is
        // cleartool rmtype [options...] type-selector...
        // as specified in the CLEARTOOL help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_RMTYPE);

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
        if (getIgnore()) {
            // -ignore
            cmd.createArgument().setValue(FLAG_IGNORE);
        }
        if (getRmAll()) {
            // -rmall -force
            cmd.createArgument().setValue(FLAG_RMALL);
            cmd.createArgument().setValue(FLAG_FORCE);
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

        // type-kind:type-name
        cmd.createArgument().setValue(getTypeSpecifier());
    }

    /**
     * Set the ignore flag
     *
     * @param ignore the status to set the flag to
     */
    public void setIgnore(boolean ignore) {
        mIgnore = ignore;
    }

    /**
     * Get ignore flag status
     *
     * @return boolean containing status of ignore flag
     */
    public boolean getIgnore() {
        return mIgnore;
    }

    /**
     * Set rmall flag
     *
     * @param rmall the status to set the flag to
     */
    public void setRmAll(boolean rmall) {
        mRmall = rmall;
    }

    /**
     * Get rmall flag status
     *
     * @return boolean containing status of rmall flag
     */
    public boolean getRmAll() {
        return mRmall;
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
     * Set type-kind string
     *
     * @param tk the type-kind string
     */
    public void setTypeKind(String tk) {
        mTypeKind = tk;
    }

    /**
     * Get type-kind string
     *
     * @return String containing the type-kind
     */
    public String getTypeKind() {
        return mTypeKind;
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
     * Get the 'type-specifier' string
     *
     * @return the 'type-kind:type-name@vob' specifier
     *
     */
    private String getTypeSpecifier() {
        String tkind = getTypeKind();
        String tname = getTypeName();

        // Return the type-selector
        String typeSpec = tkind + ":" + tname;
        if (getVOB() != null) {
            typeSpec += "@" + getVOB();
        }
        return typeSpec;
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

}
