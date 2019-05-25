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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Labels Visual SourceSafe files via a SourceOffSite server.
 *
 * @ant.task name="soslabel" category="scm"
 */
public class SOSLabel extends SOS {

    /**
     * The version number to label.
     *
     * @param  version  The new version value
     */
    public void setVersion(String version) {
        super.setInternalVersion(version);
    }

    /**
     * The label to apply the the files in SourceSafe.
     *
     * @param  label  The new label value
     *
     * @ant.attribute group="required"
     */
    public void setLabel(String label) {
        super.setInternalLabel(label);
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
     * Build the command line
     * <p>
     * AddLabel required parameters: -server -name -password -database -project -label<br>
     * AddLabel optional parameters: -verbose -comment
     * </p>
     *
     * @return    Commandline the generated command to be executed
     */
    @Override
    protected Commandline buildCmdLine() {
        commandLine = new Commandline();

        // add -command AddLabel to the commandline
        commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
        commandLine.createArgument().setValue(SOSCmd.COMMAND_LABEL);

        getRequiredAttributes();

        // a label is required
        if (getLabel() == null) {
            throw new BuildException("label attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(SOSCmd.FLAG_LABEL);
        commandLine.createArgument().setValue(getLabel());

        // -verbose
        commandLine.createArgument().setValue(getVerbose());
        // Look for a comment
        if (getComment() != null) {
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMENT);
            commandLine.createArgument().setValue(getComment());
        }
        return commandLine;
    }
}
