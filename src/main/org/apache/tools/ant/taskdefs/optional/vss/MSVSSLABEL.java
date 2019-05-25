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

/**
 * Performs Label commands to Microsoft Visual SourceSafe.
 *
 * @ant.task name="vsslabel" category="scm"
 */
public class MSVSSLABEL extends MSVSS {

    /**
     * Builds a command line to execute ss.
     * @return     The constructed commandline.
     */
    Commandline buildCmdLine() {
        Commandline commandLine = new Commandline();

        // first off, make sure that we've got a command and a vssdir and a label ...
        if (getVsspath() == null) {
            throw new BuildException("vsspath attribute must be set!", getLocation());
        }

        String label = getLabel();
        if (label.isEmpty()) {
            String msg = "label attribute must be set!";
            throw new BuildException(msg, getLocation());
        }

        // build the command line from what we got the format is
        // ss Label VSS items [-C] [-H] [-I-] [-Llabel] [-N] [-O] [-V] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_LABEL);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());
        // -C
        commandLine.createArgument().setValue(getComment());
        // -I- or -I-Y or -I-N
        commandLine.createArgument().setValue(getAutoresponse());
        // -L Specify the new label on the command line (instead of being prompted)
        commandLine.createArgument().setValue(label);
        // -V Label an existing file or project version
        commandLine.createArgument().setValue(getVersion());
        // -Y
        commandLine.createArgument().setValue(getLogin());

        return commandLine;
    }

    /**
     * Label to apply in SourceSafe.
     *
     * @param  label The label to apply.
     *
     * @ant.attribute group="required"
     */
    public void setLabel(String label) {
        super.setInternalLabel(label);
    }

    /**
     * Version to label.
     *
     * @param  version The version to label.
     */
    public void setVersion(String version) {
        super.setInternalVersion(version);
    }

    /**
     * Comment to apply to files labeled in SourceSafe.
     *
     * @param comment The comment to apply in SourceSafe
     */
    public void setComment(String comment) {
        super.setInternalComment(comment);
    }

    /**
     * Autoresponse behaviour. Valid options are Y and N.
     *
     * @param response The auto response value.
     */
    public void setAutoresponse(String response) {
        super.setInternalAutoResponse(response);
    }
}
