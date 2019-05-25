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

import java.io.IOException;

import org.apache.tools.ant.Project;

/**
 * A command launcher that proxies another command
 * launcher. Sub-classes override exec(args, env, workdir).
 */
public class CommandLauncherProxy extends CommandLauncher {
    private final CommandLauncher myLauncher;

    protected CommandLauncherProxy(CommandLauncher launcher) {
        myLauncher = launcher;
    }

    /**
     * Launches the given command in a new process. Delegates this
     * method to the proxied launcher.
     *
     * @param project
     *        the Ant project.
     * @param cmd
     *        the command line to execute as an array of strings.
     * @param env
     *        the environment to set as an array of strings.
     * @return the created Process.
     * @throws IOException
     *         forwarded from the exec method of the command launcher.
     */
    @Override
    public Process exec(Project project, String[] cmd, String[] env)
        throws IOException {
        return myLauncher.exec(project, cmd, env);
    }
}