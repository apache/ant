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
package org.apache.tools.ant.taskdefs.optional.sos;

import org.apache.tools.ant.types.Commandline;

/**
 * Commits and unlocks files in Visual SourceSafe via a SourceOffSite server.
 *
 * @ant.task name="soscheckin" category="scm"
 */
public class SOSCheckin extends SOS {

    /**
     * The filename to act upon.
     * If no file is specified then the task
     * acts upon the project.
     *
     * @param  filename  The new file value
     */
    public final void setFile(String filename) {
        super.setInternalFilename(filename);
    }

    /**
     * Flag to recursively apply the action. Defaults to false.
     *
     * @param  recursive  True for recursive operation.
     */
    public void setRecursive(boolean recursive) {
        super.setInternalRecursive(recursive);
    }

    /**
     * The comment to apply to all files being labelled.
     *
     * @param  comment  The new comment value
     */
    public void setComment(String comment) {
        super.setInternalComment(comment);
    }

    /**
     * Build the command line.
     * <p>
     * CheckInFile required parameters: -server -name -password -database -project
     *  -file<br>
     * CheckInFile optional parameters: -workdir -log -verbose -nocache -nocompression
     *  -soshome<br>
     * CheckInProject required parameters: -server -name -password -database
     *  -project<br>
     * CheckInProject optional parameters: workdir -recursive -log -verbose
     *  -nocache -nocompression -soshome
     * </p>
     *
     * @return    Commandline the generated command to be executed
     */
    @Override
    protected Commandline buildCmdLine() {
        commandLine = new Commandline();

        // If we find a "file" attribute then act on a file otherwise act on a project
        if (getFilename() != null) {
            // add -command CheckInFile to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
            commandLine.createArgument().setValue(SOSCmd.COMMAND_CHECKIN_FILE);
            // add -file xxxxx to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_FILE);
            commandLine.createArgument().setValue(getFilename());
        } else {
            // add -command CheckInProject to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
            commandLine.createArgument().setValue(SOSCmd.COMMAND_CHECKIN_PROJECT);
            // look for a recursive option
            commandLine.createArgument().setValue(getRecursive());
        }

        getRequiredAttributes();
        getOptionalAttributes();

        // Look for a comment
        if (getComment() != null) {
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMENT);
            commandLine.createArgument().setValue(getComment());
        }
        return commandLine;
    }
}
