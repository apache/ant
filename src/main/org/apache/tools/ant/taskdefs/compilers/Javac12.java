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

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * The implementation of the javac compiler for JDK 1.2
 * This is primarily a cut-and-paste from the original javac task before it
 * was refactored.
 *
 * @since Ant 1.3
 * @deprecated Use {@link Javac13} instead.
 */
@Deprecated
public class Javac12 extends DefaultCompilerAdapter {
    protected static final String CLASSIC_COMPILER_CLASSNAME = "sun.tools.javac.Main";

    /**
     * Run the compilation.
     * @return true if the compiler ran with a zero exit result (ok)
     * @exception BuildException if the compilation has problems.
     */
    public boolean execute() throws BuildException {
        attributes.log("Using classic compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupJavacCommand(true);

        try (OutputStream logstr = new LogOutputStream(attributes, Project.MSG_WARN)) {
            // Create an instance of the compiler, redirecting output to
            // the project log
            Class<?> c = Class.forName(CLASSIC_COMPILER_CLASSNAME);
            Constructor<?> cons = c.getConstructor(OutputStream.class, String.class);
            Object compiler = cons.newInstance(logstr, "javac");

            // Call the compile() method
            Method compile = c.getMethod("compile", String[].class);
            return (Boolean) compile.invoke(compiler, new Object[] {cmd.getArguments()});
        } catch (ClassNotFoundException ex) {
            throw new BuildException("Cannot use classic compiler, as it is "
                                        + "not available. \n"
                                        + " A common solution is "
                                        + "to set the environment variable"
                                        + " JAVA_HOME to your jdk directory.\n"
                                        + "It is currently set to \""
                                        + JavaEnvUtils.getJavaHome()
                                        + "\"",
                                        location);
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error starting classic compiler: ",
                                         ex, location);
            }
        }
    }
}
