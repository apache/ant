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
 * Perform Get commands from Microsoft Visual SourceSafe.
 *
 * @author Craig Cottingham
 * @author Andrew Everitt
 * @author Jesse Stockall
 *
 * @ant.task name="vssget" category="scm"
 * @ant.attribute.group name="vdl" description="Only one of version, date or label"
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
     * Override the project working directory.
     *
     * @param   localPath   The path on disk.
     */
    public void setLocalpath(Path localPath) {
        super.setInternalLocalPath(localPath.toString());
    }

    /**
     * Get files recursively. Defaults to false.
     *
     * @param recursive  The boolean value for recursive.
     */
    public final void setRecursive(boolean recursive) {
        super.setInternalRecursive(recursive);
    }

    /**
     * Enable quiet mode. Defaults to false.
     *
     * @param   quiet The boolean value for quiet.
     */
    public final void setQuiet (boolean quiet) {
        super.setInternalQuiet(quiet);
    }

    /**
     * Unset the READ-ONLY flag on files retrieved from VSS. Defaults to false.
     *
     * @param   writable The boolean value for writable.
     */
    public final void setWritable(boolean writable) {
        super.setInternalWritable(writable);
    }

    /**
     * Version to get.
     *
     * @param  version The version to get.
     *
     * @ant.attribute group="vdl"
     */
    public void setVersion(String version) {
        super.setInternalVersion(version);
    }

    /**
     * Date to get.
     *
     * @param  date The date to get.
     *
     * @ant.attribute group="vdl"
     */
    public void setDate(String date) {
        super.setInternalDate(date);
    }

    /**
     * Label to get.
     *
     * @param  label The label to get.
     *
     * @ant.attribute group="vdl"
     */
    public void setLabel(String label) {
        super.setInternalLabel(label);
    }

    /**
     * Autoresponce behaviour. Valid options are Y and N.
     *
     * @param response The auto response value.
     */
    public void setAutoresponse(String response) {
        super.setInternalAutoResponse(response);
    }

    /**
     * Date and time stamp given to the local copy. Defaults to <code>current</code>.
     *
     * @param timestamp     The file time stamping behaviour.
     */
    public void setFileTimeStamp(CurrentModUpdated timestamp) {
        super.setInternalFileTimeStamp(timestamp);
    }

    /**
     * Action taken when local files are writable. Defaults to <code>fail</code>.
     * <p>
     * Due to ss.exe returning with an exit code of '100' for both errors and when
     * a file has been skipped, <code>failonerror</code> is set to false when using
     * the <code>skip</code> option.
     *
     * @param files
     */
    public void setWritableFiles(WritableFiles files) {
        super.setInternalWritableFiles(files);
    }
}
