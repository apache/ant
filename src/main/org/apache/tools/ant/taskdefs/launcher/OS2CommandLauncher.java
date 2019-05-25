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

import org.apache.tools.ant.Project;

/**
 * A command launcher for OS/2 that uses 'cmd.exe' when launching
 * commands in directories other than the current working directory.
 *
 * <p>Unlike Windows NT and friends, OS/2's cd doesn't support the /d
 * switch to change drives and directories in one go.</p>
 */
public class OS2CommandLauncher extends CommandLauncherProxy {
    public OS2CommandLauncher(CommandLauncher launcher) {
        super(launcher);
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
        File commandDir = workingDir;
        if (workingDir == null) {
            if (project != null) {
                commandDir = project.getBaseDir();
            } else {
                return exec(project, cmd, env);
            }
        }
        // Use cmd.exe to change to the specified drive and
        // directory before running the command
        final int preCmdLength = 7;
        final String cmdDir = commandDir.getAbsolutePath();
        String[] newcmd = new String[cmd.length + preCmdLength];
        // CheckStyle:MagicNumber OFF - do not bother
        newcmd[0] = "cmd";
        newcmd[1] = "/c";
        newcmd[2] = cmdDir.substring(0, 2);
        newcmd[3] = "&&";
        newcmd[4] = "cd";
        newcmd[5] = cmdDir.substring(2);
        newcmd[6] = "&&";
        // CheckStyle:MagicNumber ON
        System.arraycopy(cmd, 0, newcmd, preCmdLength, cmd.length);

        return exec(project, newcmd, env);
    }
}