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
package org.apache.tools.ant.taskdefs.optional.javah;

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.optional.Javah;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * This implementation runs the javah executable in a separate process.
 *
 * @since Ant 1.9.8
 */
public class ForkingJavah implements JavahAdapter {

    /**
     * the name of this adapter for users to select
     */
    public static final String IMPLEMENTATION_NAME = "forking";

    /**
     * Performs the actual compilation.
     * @param javah the calling javah task.
     * @return true if the compilation was successful.
     * @throws BuildException if there is an error.
     */
    @Override
    public boolean compile(Javah javah) throws BuildException {
        Commandline cmd = SunJavah.setupJavahCommand(javah);
        Project project = javah.getProject();
        String executable = JavaEnvUtils.getJdkExecutable("javah");
        javah.log("Running " + executable, Project.MSG_VERBOSE);
        cmd.setExecutable(executable);

        //set up the args
        String[] args = cmd.getCommandline();

        try {
            Execute exe = new Execute(new LogStreamHandler(javah,
                    Project.MSG_INFO,
                    Project.MSG_WARN));
            exe.setAntRun(project);
            exe.setWorkingDirectory(project.getBaseDir());
            exe.setCommandline(args);
            exe.execute();
            return !exe.isFailure();
        } catch (IOException exception) {
            throw new BuildException("Error running " + executable
                    + " -maybe it is not on the path", exception);
        }
    }
}
