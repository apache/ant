/*
 * Copyright  2001-2004 The Apache Software Foundation
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
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the gcj compiler.
 * This is primarily a cut-and-paste from the jikes.
 *
 * @since Ant 1.4
 */
public class Gcj extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the gcj compiler.
     */
    public boolean execute() throws BuildException {
        Commandline cmd;
        attributes.log("Using gcj compiler", Project.MSG_VERBOSE);
        cmd = setupGCJCommand();

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        return
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }

    protected Commandline setupGCJCommand() {
        Commandline cmd = new Commandline();
        Path classpath = new Path(project);

        // gcj doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        // gcj doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        classpath.addExtdirs(extdirs);

        if (bootclasspath == null || bootclasspath.size() == 0) {
            // no bootclasspath, therefore, get one from the java runtime
            includeJavaRuntime = true;
        }
        classpath.append(getCompileClasspath());

        // Gcj has no option for source-path so we
        // will add it to classpath.
        if (compileSourcepath != null) {
            classpath.append(compileSourcepath);
        } else {
            classpath.append(src);
        }

        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "gcj" : exec);

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);

            if (!destDir.exists() && !destDir.mkdirs()) {
                throw new BuildException("Can't make output directories. "
                                         + "Maybe permission is wrong. ");
            }
        }

        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        if (encoding != null) {
            cmd.createArgument().setValue("--encoding=" + encoding);
        }
        if (debug) {
            cmd.createArgument().setValue("-g1");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }

        /**
         *  gcj should be set for generate class.
         */
        cmd.createArgument().setValue("-C");

        addCurrentCompilerArgs(cmd);

        return cmd;
    }
}
