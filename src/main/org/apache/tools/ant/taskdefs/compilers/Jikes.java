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
 * The implementation of the jikes compiler.
 * This is primarily a cut-and-paste from the original javac task before it
 * was refactored.
 *
 * @since Ant 1.3
 */
public class Jikes extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the Jikes compiler from IBM.
     * Mostly of this code is identical to doClassicCompile()
     * However, it does not support all options like
     * extdirs, deprecation and so on, because
     * there is no option in jikes and I don't understand
     * what they should do.
     *
     * It has been successfully tested with jikes &gt;1.10.
     * @return true if the compilation succeeded
     * @throws BuildException on error
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using jikes compiler", Project.MSG_VERBOSE);

        Commandline cmd = new Commandline();

        // For -sourcepath, use the "sourcepath" value if present.
        // Otherwise default to the "srcdir" value.
        Path sourcepath;
        if (compileSourcepath != null) {
            sourcepath = compileSourcepath;
        } else {
            sourcepath = src;
        }
        // If the buildfile specifies sourcepath="", then don't
        // output any sourcepath.
        if (!sourcepath.isEmpty()) {
            cmd.createArgument().setValue("-sourcepath");
            cmd.createArgument().setPath(sourcepath);
        }

        Path classpath = new Path(project);

        if (bootclasspath == null || bootclasspath.isEmpty()) {
            // no bootclasspath, therefore, get one from the java runtime
            includeJavaRuntime = true;
        } else {
            // there is a bootclasspath stated.  By default, the
            // includeJavaRuntime is false.  If the user has stated a
            // bootclasspath and said to include the java runtime, it's on
            // their head!
        }
        classpath.append(getCompileClasspath());

        // if the user has set JIKESPATH we should add the contents as well
        String jikesPath = System.getProperty("jikes.class.path");
        if (jikesPath != null) {
            classpath.append(new Path(project, jikesPath));
        }

        if (extdirs != null && !extdirs.isEmpty()) {
            cmd.createArgument().setValue("-extdirs");
            cmd.createArgument().setPath(extdirs);
        }

        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "jikes" : exec);

        if (deprecation) {
            cmd.createArgument().setValue("-deprecation");
        }

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);
        }

        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }
        if (debug) {
            String debugLevel = attributes.getDebugLevel();
            if (debugLevel != null) {
                cmd.createArgument().setValue("-g:" + debugLevel);
            } else {
                cmd.createArgument().setValue("-g");
            }
        } else {
            cmd.createArgument().setValue("-g:none");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }
        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }
        if (depend) {
            cmd.createArgument().setValue("-depend");
        }

        if (target != null) {
            cmd.createArgument().setValue("-target");
            cmd.createArgument().setValue(target);
        }

        addPropertyParams(cmd);

        if (attributes.getSource() != null) {
            cmd.createArgument().setValue("-source");
            String source = attributes.getSource();
            if ("1.1".equals(source) || "1.2".equals(source)) {
                // support for -source 1.1 and -source 1.2 has been
                // added with JDK 1.4.2, Jikes doesn't like it
                attributes.log("Jikes doesn't support '-source " + source
                        + "', will use '-source 1.3' instead");
                cmd.createArgument().setValue("1.3");
            } else {
                cmd.createArgument().setValue(source);
            }
        }
        addCurrentCompilerArgs(cmd);

        int firstFileName = cmd.size();

        Path boot = getBootClassPath();
        if (!boot.isEmpty()) {
            cmd.createArgument().setValue("-bootclasspath");
            cmd.createArgument().setPath(boot);
        }
        logAndAddFilesToCompile(cmd);

        return executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }

    private void addPropertyParams(Commandline cmd) {
        /*
         * TODO
         * Perhaps we shouldn't use properties for these
         * three options (emacs mode, warnings and pedantic),
         * but include it in the javac directive?
         */

        /**
         * Jikes has the nice feature to print error
         * messages in a form readable by emacs, so
         * that emacs can directly set the cursor
         * to the place, where the error occurred.
         */
        String emacsProperty = project.getProperty("build.compiler.emacs");
        if (emacsProperty != null && Project.toBoolean(emacsProperty)) {
            cmd.createArgument().setValue("+E");
        }

        /**
         * Jikes issues more warnings that javac, for
         * example, when you have files in your classpath
         * that don't exist. As this is often the case, these
         * warning can be pretty annoying.
         */
        String warningsProperty = project.getProperty("build.compiler.warnings");
        if (warningsProperty != null) {
            attributes.log("!! the build.compiler.warnings property is " + "deprecated. !!",
                    Project.MSG_WARN);
            attributes.log("!! Use the nowarn attribute instead. !!", Project.MSG_WARN);
            if (!Project.toBoolean(warningsProperty)) {
                cmd.createArgument().setValue("-nowarn");
            }
        }
        if (attributes.getNowarn()) {
            cmd.createArgument().setValue("-nowarn");
        }

        /**
         * Jikes can issue pedantic warnings.
         */
        String pedanticProperty = project.getProperty("build.compiler.pedantic");
        if (pedanticProperty != null && Project.toBoolean(pedanticProperty)) {
            cmd.createArgument().setValue("+P");
        }

        /**
         * Jikes supports something it calls "full dependency
         * checking", see the jikes documentation for differences
         * between -depend and +F.
         */
        String fullDependProperty = project.getProperty("build.compiler.fulldepend");
        if (fullDependProperty != null
            && Project.toBoolean(fullDependProperty)) {
            cmd.createArgument().setValue("+F");
        }
    }

}
