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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * A command launcher for VMS that writes the command to a temporary
 * DCL script before launching commands. This is due to limitations of
 * both the DCL interpreter and the Java VM implementation.
 */
public class VmsCommandLauncher extends Java13CommandLauncher {

    /**
     * Launches the given command in a new process.
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
        File cmdFile = createCommandFile(project, cmd, env);
        Process p =
            super.exec(project, new String[] {cmdFile.getPath()}, env);
        deleteAfter(cmdFile, p);
        return p;
    }

    /**
     * Launches the given command in a new process, in the given
     * working directory. Note that under Java 1.4.0 and 1.4.1 on VMS
     * this method only works if <code>workingDir</code> is null or
     * the logical JAVA$FORK_SUPPORT_CHDIR needs to be set to TRUE.
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
        File cmdFile = createCommandFile(project, cmd, env);
        Process p = super.exec(project, new String[] {cmdFile.getPath()}, env,
            workingDir);
        deleteAfter(cmdFile, p);
        return p;
    }

    /*
     * Writes the command into a temporary DCL script and returns the
     * corresponding File object.  The script will be deleted on exit.
     * @param cmd the command line to execute as an array of strings.
     * @param env the environment to set as an array of strings.
     * @return the command File.
     * @throws IOException if errors are encountered creating the file.
     */
    private File createCommandFile(final Project project, String[] cmd, String[] env)
        throws IOException {
        File script = FILE_UTILS.createTempFile(project, "ANT", ".COM", null, true, true);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(script))) {

            // add the environment as logicals to the DCL script
            if (env != null) {
                int eqIndex;
                for (String variable : env) {
                    eqIndex = variable.indexOf('=');
                    if (eqIndex != -1) {
                        out.write("$ DEFINE/NOLOG ");
                        out.write(variable.substring(0, eqIndex));
                        out.write(" \"");
                        out.write(variable.substring(eqIndex + 1));
                        out.write('\"');
                        out.newLine();
                    }
                }
            }
            out.write("$ " + cmd[0]);
            for (int i = 1; i < cmd.length; i++) {
                out.write(" -");
                out.newLine();
                out.write(cmd[i]);
            }
        }
        return script;
    }

    private void deleteAfter(final File f, final Process p) {
        new Thread(() -> {
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                // ignore
            }
            FileUtils.delete(f);
        }).start();
    }
}
