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

package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * Perform Get commands from Microsoft Visual SourceSafe.
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
    public final void setQuiet(boolean quiet) {
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
     * Autoresponse behaviour. Valid options are Y and N.
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
     * @param files The action to take.
     */
    public void setWritableFiles(WritableFiles files) {
        super.setInternalWritableFiles(files);
    }
}
