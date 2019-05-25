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


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;


/**
 * Task allows to reconfigure a project, recursively or not
 */
public class CCMReconfigure extends Continuus {
    /**
     * /recurse --
     */
    public static final String FLAG_RECURSE = "/recurse";

    /**
     * /recurse --
     */
    public static final String FLAG_VERBOSE = "/verbose";


    /**
     *  /project flag -- target project
     */
    public static final String FLAG_PROJECT = "/project";

    private String ccmProject = null;
    private boolean recurse = false;
    private boolean verbose = false;

    /** Constructor for CCMReconfigure. */
    public CCMReconfigure() {
        super();
        setCcmAction(COMMAND_RECONFIGURE);
    }

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ccm and then calls Exec's run method
     * to execute the command line.
     * </p>
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());

        checkOptions(commandLine);

        int result = run(commandLine);
        if (Execute.isFailure(result)) {
            throw new BuildException("Failed executing: " + commandLine,
                getLocation());
        }
    }

    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {

        if (isRecurse()) {
            cmd.createArgument().setValue(FLAG_RECURSE);
        }

        if (isVerbose()) {
            cmd.createArgument().setValue(FLAG_VERBOSE);
        }

        if (getCcmProject() != null) {
            cmd.createArgument().setValue(FLAG_PROJECT);
            cmd.createArgument().setValue(getCcmProject());
        }

    }

    /**
     * Get the value of project.
     * @return value of project.
     */
    public String getCcmProject() {
        return ccmProject;
    }

    /**
     * Sets the ccm project on which the operation is applied.
     * @param v  Value to assign to project.
     */
    public void setCcmProject(String v) {
        this.ccmProject = v;
    }

    /**
     * Get the value of recurse.
     * @return value of recurse.
     */
    public boolean isRecurse() {
        return recurse;
    }

    /**
     * If true, recurse on subproject (default false).
     *
     * @param v  Value to assign to recurse.
     */
    public void setRecurse(boolean v) {
        this.recurse = v;
    }

    /**
     * Get the value of verbose.
     * @return value of verbose.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * If true, do a verbose reconfigure operation (default false).
     * @param v  Value to assign to verbose.
     */
    public void setVerbose(boolean v) {
        this.verbose = v;
    }

}
