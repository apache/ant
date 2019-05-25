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
package org.apache.tools.ant.taskdefs.launcher;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * A command launcher for JDK/JRE 1.3 (and higher). Uses the built-in
 * Runtime.exec() command.
 */
public class Java13CommandLauncher extends CommandLauncher {

    /**
     * Launches the given command in a new process, in the given
     * working directory.
     *
     * @param project
     *        the Ant project.
     * @param cmd
     *        the command line to execute as an array of strings.
     * @param env
     *        the environment to set as an array of strings.
     * @param workingDir
     *        the working directory where the command should run.
     * @return the created Process.
     * @throws IOException
     *         probably forwarded from Runtime#exec.
     */
    @Override
    public Process exec(Project project, String[] cmd, String[] env,
                        File workingDir) throws IOException {
        try {
            if (project != null) {
                project.log("Execute:Java13CommandLauncher: "
                            + Commandline.describeCommand(cmd),
                            Project.MSG_DEBUG);
            }
            return Runtime.getRuntime().exec(cmd, env, workingDir);
        } catch (IOException ioex) {
            throw ioex;
        } catch (Exception exc) {
            // IllegalAccess, IllegalArgument, ClassCast
            throw new BuildException("Unable to execute command", exc);
        }
    }
}