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
 * The implementation of the jikes compiler.
 * This is primarily a cut-and-paste from the original javac task before it
 * was refactored.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green
 *         <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author Stefan Bodewig
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @since Ant 1.3
 */
public class Jikes extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the Jikes compiler from IBM.
     * Mostly of this code is identical to doClassicCompile()
     * However, it does not support all options like
     * bootclasspath, extdirs, deprecation and so on, because
     * there is no option in jikes and I don't understand
     * what they should do.
     *
     * It has been successfully tested with jikes >1.10
     */
    public boolean execute() throws BuildException {
        attributes.log("Using jikes compiler", Project.MSG_VERBOSE);

        Path classpath = new Path(project);

        // Jikes doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        // Jikes doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        classpath.addExtdirs(extdirs);

        if (bootclasspath == null || bootclasspath.size() == 0) {
            // no bootclasspath, therefore, get one from the java runtime
            includeJavaRuntime = true;
        } else {
            // there is a bootclasspath stated.  By default, the
            // includeJavaRuntime is false.  If the user has stated a
            // bootclasspath and said to include the java runtime, it's on
            // their head!
        }
        classpath.append(getCompileClasspath());

        // Jikes has no option for source-path so we
        // will add it to classpath.
        if (compileSourcepath != null) {
            classpath.append(compileSourcepath);
        } else {
            classpath.append(src);
        }

        // if the user has set JIKESPATH we should add the contents as well
        String jikesPath = System.getProperty("jikes.class.path");
        if (jikesPath != null) {
            classpath.append(new Path(project, jikesPath));
        }

        Commandline cmd = new Commandline();
        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "jikes" : exec);

        if (deprecation == true) {
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
            cmd.createArgument().setValue("-g");
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

        /**
         * XXX
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
        String warningsProperty =
            project.getProperty("build.compiler.warnings");
        if (warningsProperty != null) {
            attributes.log("!! the build.compiler.warnings property is "
                           + "deprecated. !!", Project.MSG_WARN);
            attributes.log("!! Use the nowarn attribute instead. !!",
                           Project.MSG_WARN);
            if (!Project.toBoolean(warningsProperty)) {
                cmd.createArgument().setValue("-nowarn");
            }
        } if (attributes.getNowarn()) {
            /*
             * FIXME later
             *
             * let the magic property win over the attribute for backwards
             * compatibility
             */
            cmd.createArgument().setValue("-nowarn");
        }

        /**
         * Jikes can issue pedantic warnings.
         */
        String pedanticProperty =
            project.getProperty("build.compiler.pedantic");
        if (pedanticProperty != null && Project.toBoolean(pedanticProperty)) {
            cmd.createArgument().setValue("+P");
        }

        /**
         * Jikes supports something it calls "full dependency
         * checking", see the jikes documentation for differences
         * between -depend and +F.
         */
        String fullDependProperty =
            project.getProperty("build.compiler.fulldepend");
        if (fullDependProperty != null
            && Project.toBoolean(fullDependProperty)) {
            cmd.createArgument().setValue("+F");
        }

        if (attributes.getSource() != null) {
            cmd.createArgument().setValue("-source");
            cmd.createArgument().setValue(attributes.getSource());
        }

        addCurrentCompilerArgs(cmd);

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        return
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }


}
