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

package org.apache.tools.ant.taskdefs.optional.ccm;

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;


/**
 * A base class for creating tasks for executing commands on Continuus 5.1.
 * <p>
 * The class extends the  task as it operates by executing the ccm.exe program
 * supplied with Continuus/Synergy. By default the task expects the ccm executable to be
 * in the path,
 * you can override this be specifying the ccmdir attribute.
 * </p>
 *
 */
public abstract class Continuus extends Task {
    /**
     * Constant for the thing to execute
     */
    private static final String CCM_EXE = "ccm";

    /**
     * The 'CreateTask' command
     */
    public static final String COMMAND_CREATE_TASK = "create_task";
    /**
     * The 'Checkout' command
     */
    public static final String COMMAND_CHECKOUT = "co";
    /**
     * The 'Checkin' command
     */
    public static final String COMMAND_CHECKIN = "ci";
    /**
     * The 'Reconfigure' command
     */
    public static final String COMMAND_RECONFIGURE = "reconfigure";

    /**
     * The 'Reconfigure' command
     */
    public static final String COMMAND_DEFAULT_TASK = "default_task";

    private String ccmDir = "";
    private String ccmAction = "";

    /**
     * Get the value of ccmAction.
     * @return value of ccmAction.
     */
    public String getCcmAction() {
        return ccmAction;
    }

    /**
     * Set the value of ccmAction.
     * @param v  Value to assign to ccmAction.
     * @ant.attribute ignore="true"
     */
    public void setCcmAction(String v) {
        this.ccmAction = v;
    }

    /**
     * Set the directory where the ccm executable is located.
     *
     * @param dir the directory containing the ccm executable
     */
    public final void setCcmDir(String dir) {
        ccmDir = FileUtils.translatePath(dir);
    }

    /**
     * Builds and returns the command string to execute ccm
     * @return String containing path to the executable
     */
    protected final String getCcmCommand() {
        String toReturn = ccmDir;
        if (!toReturn.isEmpty() && !toReturn.endsWith("/")) {
            toReturn += "/";
        }

        toReturn += CCM_EXE;

        return toReturn;
    }

    /**
     * Run the command.
     * @param cmd the command line
     * @param handler an execute stream handler
     * @return the exit status of the command
     */
    protected int run(Commandline cmd, ExecuteStreamHandler handler) {
        try {
            Execute exe = new Execute(handler);
            exe.setAntRun(getProject());
            exe.setWorkingDirectory(getProject().getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    /**
     * Run the command.
     * @param cmd the command line
     * @return the exit status of the command
     */
    protected int run(Commandline cmd) {
        return run(cmd, new LogStreamHandler(this, Project.MSG_VERBOSE, Project.MSG_WARN));
    }

}
