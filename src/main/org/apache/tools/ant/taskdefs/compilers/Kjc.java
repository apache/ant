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
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the Java compiler for KJC.
 * This is primarily a cut-and-paste from Jikes.java and
 * DefaultCompilerAdapter.
 *
 * @since Ant 1.4
 */
public class Kjc extends DefaultCompilerAdapter {

    /**
     * Run the compilation.
     * @return true if the compilation succeeded
     * @exception BuildException if the compilation has problems.
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using kjc compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupKjcCommand();
        cmd.setExecutable("at.dms.kjc.Main");
        ExecuteJava ej = new ExecuteJava();
        ej.setJavaCommand(cmd);
        return ej.fork(getJavac()) == 0;
    }

    /**
     * setup kjc command arguments.
     * @return the command line
     */
    protected Commandline setupKjcCommand() {
        Commandline cmd = new Commandline();

        // generate classpath, because kjc doesn't support sourcepath.
        Path classpath = getCompileClasspath();

        if (deprecation) {
            cmd.createArgument().setValue("-deprecation");
        }

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);
        }

        // generate the classpath
        cmd.createArgument().setValue("-classpath");

        Path cp = new Path(project);

        // kjc don't have bootclasspath option.
        Path p = getBootClassPath();
        if (!p.isEmpty()) {
            cp.append(p);
        }

        if (extdirs != null) {
            cp.addExtdirs(extdirs);
        }

        cp.append(classpath);
        if (compileSourcepath != null) {
            cp.append(compileSourcepath);
        } else {
            cp.append(src);
        }

        cmd.createArgument().setPath(cp);

        // kjc-1.5A doesn't support -encoding option now.
        // but it will be supported near the feature.
        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }

        if (debug) {
            cmd.createArgument().setValue("-g");
        }

        if (optimize) {
            cmd.createArgument().setValue("-O2");
        }

        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }

        addCurrentCompilerArgs(cmd);

        logAndAddFilesToCompile(cmd);
        return cmd;
    }
}
