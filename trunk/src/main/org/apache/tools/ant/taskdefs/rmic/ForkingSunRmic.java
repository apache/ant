/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.taskdefs.Rmic;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

import java.io.IOException;

/**
 * This is an extension of the sun rmic compiler, which forks rather than
 * executes it inline. Why so? Because rmic is dog slow, but if you fork the
 * compiler you can have multiple copies compiling different bits of your project
 * at the same time. Which, on a multi-cpu system results in significant speedups.
 *
 * Also, Java1.6 behaves oddly with -XNew, so we switch it on here if needed.
 * @since ant1.7
 */
public class ForkingSunRmic extends DefaultRmicAdapter {

    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "forking";

    /**
     * exec by creating a new command
     * @return true if the command ran successfully
     * @throws BuildException on error
     */
    public boolean execute() throws BuildException {
        Rmic owner = getRmic();
        Commandline cmd = setupRmicCommand();
        Project project = owner.getProject();
        //rely on RMIC being on the path
        cmd.setExecutable(JavaEnvUtils.getJdkExecutable(getExecutableName()));

        //set up the args
        String[] args = cmd.getCommandline();

        try {
            Execute exe = new Execute(new LogStreamHandler(owner,
                    Project.MSG_INFO,
                    Project.MSG_WARN));
            exe.setAntRun(project);
            exe.setWorkingDirectory(project.getBaseDir());
            exe.setCommandline(args);
            exe.execute();
            return !exe.isFailure();
        } catch (IOException exception) {
            throw new BuildException("Error running " + getExecutableName()
                    + " -maybe it is not on the path", exception);
        }
    }

    /**
     * Override point.
     * @return the executable name.
     */
    protected String getExecutableName() {
        return SunRmic.RMIC_EXECUTABLE;
    }
}
