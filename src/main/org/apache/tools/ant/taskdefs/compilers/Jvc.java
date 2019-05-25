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
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the jvc compiler from microsoft.
 * This is primarily a cut-and-paste from the original javac task before it
 * was refactored.
 *
 * @since Ant 1.3
 */
public class Jvc extends DefaultCompilerAdapter {

    /**
     * Run the compilation.
     * @return true if the compiler ran with a zero exit result (ok)
     * @exception BuildException if the compilation has problems.
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using jvc compiler", Project.MSG_VERBOSE);

        Path classpath = new Path(project);

        // jvc doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        Path p = getBootClassPath();
        if (!p.isEmpty()) {
            classpath.append(p);
        }

        if (includeJavaRuntime) {
            // jvc doesn't support an extension dir (-extdir)
            // so we'll emulate it for compatibility and convenience.
            classpath.addExtdirs(extdirs);
        }

        classpath.append(getCompileClasspath());

        // jvc has no option for source-path so we
        // will add it to classpath.
        if (compileSourcepath != null) {
            classpath.append(compileSourcepath);
        } else {
            classpath.append(src);
        }

        Commandline cmd = new Commandline();
        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "jvc" : exec);

        if (destDir != null) {
            cmd.createArgument().setValue("/d");
            cmd.createArgument().setFile(destDir);
        }

        // Add the Classpath before the "internal" one.
        cmd.createArgument().setValue("/cp:p");
        cmd.createArgument().setPath(classpath);

        boolean msExtensions = true;
        String mse = getProject().getProperty("build.compiler.jvc.extensions");
        if (mse != null) {
            msExtensions = Project.toBoolean(mse);
        }

        if (msExtensions) {
            // Enable MS-Extensions and ...
            cmd.createArgument().setValue("/x-");
            // ... do not display a Message about this.
            cmd.createArgument().setValue("/nomessage");
        }

        // Do not display Logo
        cmd.createArgument().setValue("/nologo");

        if (debug) {
            cmd.createArgument().setValue("/g");
        }
        if (optimize) {
            cmd.createArgument().setValue("/O");
        }
        if (verbose) {
            cmd.createArgument().setValue("/verbose");
        }

        addCurrentCompilerArgs(cmd);

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        return
            executeExternalCompile(cmd.getCommandline(), firstFileName,
                                   false) == 0;
    }
}
