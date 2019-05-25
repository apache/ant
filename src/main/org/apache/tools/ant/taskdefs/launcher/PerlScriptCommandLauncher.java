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

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;

/**
 * A command launcher that uses an auxiliary perl script to launch
 * commands in directories other than the current working directory.
 */
public class PerlScriptCommandLauncher extends CommandLauncherProxy {
    private final String myScript;

    public PerlScriptCommandLauncher(String script, CommandLauncher launcher) {
        super(launcher);
        myScript = script;
    }

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
     *        working directory where the command should run.
     * @return the created Process.
     * @throws IOException
     *         forwarded from the exec method of the command launcher.
     */
    @Override
    public Process exec(Project project, String[] cmd, String[] env,
                        File workingDir) throws IOException {
        if (project == null) {
            if (workingDir == null) {
                return exec(project, cmd, env);
            }
            throw new IOException(
                "Cannot locate antRun script: No project provided");
        }
        // Locate the auxiliary script
        String antHome = project.getProperty(MagicNames.ANT_HOME);
        if (antHome == null) {
            throw new IOException("Cannot locate antRun script: Property '"
                + MagicNames.ANT_HOME + "' not found");
        }
        String antRun = FILE_UTILS.resolveFile(project.getBaseDir(),
                                               antHome + File.separator
                                               + myScript).toString();

        // Build the command
        File commandDir = workingDir;
        if (workingDir == null) {
            commandDir = project.getBaseDir();
        }
        // CheckStyle:MagicNumber OFF
        String[] newcmd = new String[cmd.length + 3];
        newcmd[0] = "perl";
        newcmd[1] = antRun;
        newcmd[2] = commandDir.getAbsolutePath();
        System.arraycopy(cmd, 0, newcmd, 3, cmd.length);
        // CheckStyle:MagicNumber ON

        return exec(project, newcmd, env);
    }
}