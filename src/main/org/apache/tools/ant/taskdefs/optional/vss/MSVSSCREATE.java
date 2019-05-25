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
 * Creates a new project in Microsoft Visual SourceSafe.
 *
 * @ant.task name="vsscreate" category="scm"
 */
public class MSVSSCREATE extends MSVSS {

    /**
     * Builds a command line to execute ss.
     * @return     The constructed commandline.
     */
    Commandline buildCmdLine() {
        Commandline commandLine = new Commandline();

        // first off, make sure that we've got a command and a vssdir...
        if (getVsspath() == null) {
            String msg = "vsspath attribute must be set!";
            throw new BuildException(msg, getLocation());
        }

        // build the command line from what we got
        // the format is:
        // ss Create VSS items [-C] [-H] [-I-] [-N] [-O] [-S] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_CREATE);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());
        // -C
        commandLine.createArgument().setValue(getComment());
        // -I- or -I-Y or -I-N
        commandLine.createArgument().setValue(getAutoresponse());
        // -O-
        commandLine.createArgument().setValue(getQuiet());
        // -Y
        commandLine.createArgument().setValue(getLogin());

        return commandLine;
    }

    /**
     * Comment to apply to the project created in SourceSafe.
     *
     * @param comment The comment to apply in SourceSafe
     */
    public void setComment(String comment) {
        super.setInternalComment(comment);
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
     * Autoresponse behaviour. Valid options are Y and N.
     *
     * @param response The auto response value.
     */
    public void setAutoresponse(String response) {
        super.setInternalAutoResponse(response);
    }
}
