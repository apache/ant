/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Perform Get commands to Microsoft Visual SourceSafe.
 * <p>
 * The following attributes are interpreted:
 * <table border="1">
 *   <tr>
 *     <th>Attribute</th>
 *     <th>Values</th>
 *     <th>Required</th>
 *   </tr>
 *   <tr>
 *      <td>login</td>
 *      <td>username,password</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>vsspath</td>
 *      <td>SourceSafe path</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>localpath</td>
 *      <td>Override the working directory and get to the specified path</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>writable</td>
 *      <td>true or false</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>recursive</td>
 *      <td>true or false</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>version</td>
 *      <td>a version number to get</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>date</td>
 *      <td>a date stamp to get at</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>label</td>
 *      <td>a label to get for</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>quiet</td>
 *      <td>suppress output (off by default)</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>autoresponse</td>
 *      <td>What to respond with (sets the -I option). By default, -I- is
 *      used; values of Y or N will be appended to this.</td>
 *      <td>No</td>
 *   </tr>
 * </table>
 * <p>Note that only one of version, date or label should be specified</p>
 *
 * @author Craig Cottingham
 * @author Andrew Everitt
 * @author Jesse Stockall
 *
 * @ant.task name="vssget" category="scm"
 */
public class MSVSSGET extends MSVSS {

    /**
     * Builds a command line to execute ss.
     * @return     The constructed commandline.
     */
    Commandline buildCmdLine() {
        Commandline commandLine = new Commandline();

        // build the command line from what we got the format is
        // ss Get VSS items [-G] [-H] [-I-] [-N] [-O] [-R] [-V] [-W] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_GET);

        if (getVsspath() == null) {
            throw new BuildException("vsspath attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(getVsspath());

        // -GL
        commandLine.createArgument().setValue(getLocalpath());
        // -I- or -I-Y or -I-N
        commandLine.createArgument().setValue(getAutoresponse());
        // -O-
        commandLine.createArgument().setValue(getQuiet());
        // -R
        commandLine.createArgument().setValue(getRecursive());
        // -V
        commandLine.createArgument().setValue(getVersionDateLabel());
        // -W
        commandLine.createArgument().setValue(getWritable());
        // -Y
        commandLine.createArgument().setValue(getLogin());
        // -G
        commandLine.createArgument().setValue(getFileTimeStamp());
        // -GWS or -GWR
        commandLine.createArgument().setValue(getWritableFiles());

        return commandLine;
    }

    /**
     * Set the local path; optional.
     * <p>
     * This is the path to override the project
     * working directory.
     * @param   localPath   The path on disk.
     */
    public void setLocalpath(Path localPath) {
        super.setInternalLocalPath(localPath.toString());
    }

    /**
     * Flag to tell the task to recurse down the tree;
     * optional, default false.
     * @param recursive  The boolean value for recursive.
     */
    public final void setRecursive(boolean recursive) {
        super.setInternalRecursive(recursive);
    }

    /**
     * Sets/clears quiet mode; optional, default false.
     * @param   quiet The boolean value for quiet.
     */
    public final void setQuiet (boolean quiet) {
        super.setInternalQuiet(quiet);
    }

    /**
     * Sets behaviour, unset the READ-ONLY flag on files retrieved from VSS.; optional, default false
     * @param   writable The boolean value for writable.
     */
    public final void setWritable(boolean writable) {
        super.setInternalWritable(writable);
    }

    /**
     * Sets the stored version string.; optional.
     * @param  version The version to get.
     */
    public void setVersion(String version) {
        super.setInternalVersion(version);
    }

    /**
     * Sets the stored date string.; optional.
     * @param  date The date to checkout.
     */
    public void setDate(String date) {
        super.setInternalDate(date);
    }

    /**
     * Sets the label to apply in SourceSafe.; optional.
     * @param  label The label to apply.
     */
    public void setLabel(String label) {
        super.setInternalLabel(label);
    }

    /**
     * Sets the autoresponce behaviour.; optional.
     * <p>
     * Valid options are Y and N.
     * @param response The auto response value.
     */
    public void setAutoresponse(String response){
        super.setInternalAutoResponse(response);
    }

    /**
     * Set the behavior for timestamps of local files.; optional
     *
     * Valid options are <code>current</code>, <code>modified</code>, or
     * <code>updated</code>. Defaults to <code>current</code>.
     *
     * @param timestamp     The file time stamping behaviour.
     */
    public void setFileTimeStamp(CurrentModUpdated timestamp) {
        super.setInternalFileTimeStamp(timestamp);
    }

    /**
     * Set the behavior when local files are writable.; optional
     *
     * Valid options are <code>replace</code>, <code>skip</code> and <code>fail</code>.
     * Defaults to <code>fail</code>
     *
     * Due to ss.exe returning with an exit code of '100' for both errors and when
     * a file has been skipped, <code>failonerror</code> is set to false when using
     * the <code>skip</code> option
     *
     * @param files
     */
    public void setWritableFiles(WritableFiles files) {
        super.setInternalWritableFiles(files);
    }
}
