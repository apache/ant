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

import static org.apache.tools.ant.MagicNames.ANT_SHELL_LAUNCHER_REF_ID;
import static org.apache.tools.ant.MagicNames.ANT_VM_LAUNCHER_REF_ID;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;

/**
 * A command launcher for a particular JVM/OS platform. This class is
 * a general purpose command launcher which can only launch commands
 * in the current working directory.
 */
public class CommandLauncher {

    protected static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static CommandLauncher vmLauncher = null;
    private static CommandLauncher shellLauncher = null;

    static {
        if (!Os.isFamily("os/2")) {
            vmLauncher = new Java13CommandLauncher();
        }

        if (Os.isFamily("mac") && !Os.isFamily("unix")) {
            // Mac
            shellLauncher = new MacCommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("os/2")) {
            // OS/2
            shellLauncher = new OS2CommandLauncher(new CommandLauncher());
        } else if (Os.isFamily("windows")) {
            CommandLauncher baseLauncher = new CommandLauncher();

            if (!Os.isFamily("win9x")) {
                // Windows XP/2000/NT
                shellLauncher = new WinNTCommandLauncher(baseLauncher);
            } else {
                // Windows 98/95 - need to use an auxiliary script
                shellLauncher =
                    new ScriptCommandLauncher("bin/antRun.bat", baseLauncher);
            }
        } else if (Os.isFamily("netware")) {

            CommandLauncher baseLauncher = new CommandLauncher();

            shellLauncher =
                new PerlScriptCommandLauncher("bin/antRun.pl", baseLauncher);
        } else if (Os.isFamily("openvms")) {
            // OpenVMS
            shellLauncher = new VmsCommandLauncher();
        } else {
            // Generic
            shellLauncher = new ScriptCommandLauncher("bin/antRun",
                new CommandLauncher());
        }
    }

    /**
     * Launches the given command in a new process.
     *
     * @param project
     *        The project that the command is part of.
     * @param cmd
     *        The command to execute.
     * @param env
     *        The environment for the new process. If null, the
     *        environment of the current process is used.
     * @return the created Process.
     * @throws IOException
     *         if attempting to run a command in a specific directory.
     */
    public Process exec(Project project, String[] cmd, String[] env)
        throws IOException {
        if (project != null) {
            project.log("Execute:CommandLauncher: "
                + Commandline.describeCommand(cmd), Project.MSG_DEBUG);
        }
        return Runtime.getRuntime().exec(cmd, env);
    }

    /**
     * Launches the given command in a new process, in the given
     * working directory.
     *
     * @param project
     *        The project that the command is part of.
     * @param cmd
     *        The command to execute.
     * @param env
     *        The environment for the new process. If null, the
     *        environment of the current process is used.
     * @param workingDir
     *        The directory to start the command in. If null, the
     *        current directory is used.
     * @return the created Process.
     * @throws IOException
     *         if trying to change directory.
     */
    public Process exec(Project project, String[] cmd, String[] env,
                        File workingDir) throws IOException {
        if (workingDir == null) {
            return exec(project, cmd, env);
        }
        throw new IOException(
            "Cannot execute a process in different directory under this JVM");
    }

    /**
     * Obtains the shell launcher configured for the given project or
     * the default shell launcher.
     *
     * @param project Project
     * @return CommandLauncher
     */
    public static CommandLauncher getShellLauncher(Project project) {
        CommandLauncher launcher = extractLauncher(ANT_SHELL_LAUNCHER_REF_ID,
                                                   project);
        if (launcher == null) {
            launcher = shellLauncher;
        }

        return launcher;
    }

    /**
     * Obtains the VM launcher configured for the given project or
     * the default VM launcher.
     *
     * @param project Project
     * @return CommandLauncher
     */
    public static CommandLauncher getVMLauncher(Project project) {
        CommandLauncher launcher = extractLauncher(ANT_VM_LAUNCHER_REF_ID,
                                                   project);
        if (launcher == null) {
            launcher = vmLauncher;
        }
        return launcher;
    }

    private static CommandLauncher extractLauncher(String referenceName,
                                                   Project project) {
        return Optional.ofNullable(project)
            .map(p -> p.<CommandLauncher> getReference(referenceName))
            .orElseGet(() -> getSystemLauncher(referenceName));
    }

    private static CommandLauncher getSystemLauncher(String launcherRefId) {
        String launcherClass = System.getProperty(launcherRefId);
        if (launcherClass != null) {
            try {
                return Class.forName(launcherClass).asSubclass(CommandLauncher.class)
                        .getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException
                    | ClassNotFoundException
                    | NoSuchMethodException | InvocationTargetException e) {
                System.err.println("Could not instantiate launcher class "
                    + launcherClass + ": " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Sets the VM launcher to use for the given project.
     *
     * @param project Project
     * @param launcher CommandLauncher
     */
    public static void setVMLauncher(Project project,
                                     CommandLauncher launcher) {
        if (project != null) {
            project.addReference(ANT_VM_LAUNCHER_REF_ID, launcher);
        }
    }

    /**
     * Sets the shell launcher to use for the given project.
     *
     * @param project Project
     * @param launcher CommandLauncher
     */
    public static void setShellLauncher(Project project,
                                        CommandLauncher launcher) {
        if (project != null) {
            project.addReference(ANT_SHELL_LAUNCHER_REF_ID, launcher);
        }
    }

}