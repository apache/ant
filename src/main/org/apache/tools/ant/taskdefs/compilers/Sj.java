/* 
 * Copyright  2001-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the sj compiler.
 * Uses the defaults for DefaultCompilerAdapter
 *
 * @author <a href="mailto:don@bea.com">Don Ferguson</a>
 * @since Ant 1.4
 */
public class Sj extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the sj compiler from Symantec.
     */
    public boolean execute() throws BuildException {
        attributes.log("Using symantec java compiler", Project.MSG_VERBOSE);

        Commandline cmd = setupJavacCommand();
        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "sj" : exec);

        int firstFileName = cmd.size() - compileList.length;

        return
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }


}

