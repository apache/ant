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
 * The implementation of the gcj compiler.
 * This is primarily a cut-and-paste from the jikes.
 *
 * @since Ant 1.4
 */
public class Gcj extends DefaultCompilerAdapter {
    private static final String[] CONFLICT_WITH_DASH_C = {"-o", "--main=", "-D", "-fjni", "-L"};

    /**
     * Performs a compile using the gcj compiler.
     * @return true if the compilation succeeded
     * @throws BuildException on error
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using gcj compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupGCJCommand();

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        return
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }

    /**
     * Set up the gcj commandline.
     * @return the command line
     */
    protected Commandline setupGCJCommand() {
        Commandline cmd = new Commandline();
        Path classpath = new Path(project);

        // gcj doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        Path p = getBootClassPath();
        if (!p.isEmpty()) {
            classpath.append(p);
        }

        // gcj doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        if (extdirs != null || includeJavaRuntime) {
            classpath.addExtdirs(extdirs);
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

            if (!destDir.exists()
                && !(destDir.mkdirs() || destDir.isDirectory())) {
                throw new BuildException(
                    "Can't make output directories. Maybe permission is wrong.");
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
         * ... if no 'compile to native' argument is passed
         */
        if (!isNativeBuild()) {
            cmd.createArgument().setValue("-C");
        }

        if (attributes.getSource() != null) {
            String source = attributes.getSource();
            cmd.createArgument().setValue("-fsource=" + source);
        }

        if (attributes.getTarget() != null) {
            String target = attributes.getTarget();
            cmd.createArgument().setValue("-ftarget=" + target);
        }

        addCurrentCompilerArgs(cmd);

        return cmd;
    }

    /**
     * Whether any of the arguments given via &lt;compilerarg&gt;
     * implies that compilation to native code is requested.
     * @return true if compilation to native code is requested
     * @since Ant 1.6.2
     */
    public boolean isNativeBuild() {
        boolean nativeBuild = false;
        String[] additionalArguments = getJavac().getCurrentCompilerArgs();
        int argsLength = 0;
        while (!nativeBuild && argsLength < additionalArguments.length) {
            int conflictLength = 0;
            while (!nativeBuild && conflictLength < CONFLICT_WITH_DASH_C.length) {
                nativeBuild = additionalArguments[argsLength]
                        .startsWith(CONFLICT_WITH_DASH_C[conflictLength]);
                conflictLength++;
            }
            argsLength++;
        }
        return nativeBuild;
    }

}
