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
 * Retrieves and locks files in Visual SourceSafe via a SourceOffSite server.
 *
 * @ant.task name="soscheckout" category="scm"
 */
public class SOSCheckout extends SOS {

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
     * Build the command line
     * <p>
     * CheckOutFile required parameters: -server -name -password -database -project -file<br>
     * CheckOutFile optional parameters: -workdir -verbose -nocache -nocompression -soshome<br>
     *
     * CheckOutProject required parameters: -server -name -password -database -project<br>
     * CheckOutProject optional parameters:-workdir -recursive -verbose -nocache
     * -nocompression -soshome
     * </p>
     *
     * @return    Commandline the generated command to be executed
     */
    @Override
    protected Commandline buildCmdLine() {
        commandLine = new Commandline();

        // If we find a "file" attribute then act on a file otherwise act on a project
        if (getFilename() != null) {
            // add -command CheckOutFile to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
            commandLine.createArgument().setValue(SOSCmd.COMMAND_CHECKOUT_FILE);
            // add -file xxxxx to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_FILE);
            commandLine.createArgument().setValue(getFilename());
        } else {
            // add -command CheckOutProject to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
            commandLine.createArgument().setValue(SOSCmd.COMMAND_CHECKOUT_PROJECT);
            // look for a recursive option
            commandLine.createArgument().setValue(getRecursive());
        }

        getRequiredAttributes();
        getOptionalAttributes();

        return commandLine;
    }
}
