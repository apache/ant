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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the sj compiler.
 * Uses the defaults for DefaultCompilerAdapter
 *
 * @since Ant 1.4
 */
public class Sj extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the sj compiler from Symantec.
     * @return true if the compilation succeeded
     * @throws BuildException on error
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using symantec java compiler", Project.MSG_VERBOSE);

        Commandline cmd = setupJavacCommand();
        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "sj" : exec);

        int firstFileName = cmd.size() - compileList.length;

        return
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }

    /**
     * Returns null since sj either has -g for debug=true or no
     * argument at all.
     * @return null.
     * @since Ant 1.6.3
     */
    @Override
    protected String getNoDebugArgument() {
        return null;
    }
}
