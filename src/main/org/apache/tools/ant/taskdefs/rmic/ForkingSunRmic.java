/*
 * Copyright  2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
 * @since ant1.7
 */
public class ForkingSunRmic extends DefaultRmicAdapter {

    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "forking";

    /**
     * exec by creating a new command
     * @return
     * @throws BuildException
     */
    public boolean execute() throws BuildException {
        Rmic owner = getRmic();
        Commandline cmd = setupRmicCommand();
        Project project = owner.getProject();
        //rely on RMIC being on the path
        cmd.setExecutable(JavaEnvUtils.getJdkExecutable(SunRmic.RMIC_EXECUTABLE));

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
            return exe.getExitValue() == 0;
        } catch (IOException exception) {
            throw new BuildException("Error running " + SunRmic.RMIC_EXECUTABLE
                    + " -maybe it is not on the path", exception);
        }
    }
}
