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

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;


/**
 * The implementation of the javac compiler for JDK 1.3
 * This is primarily a cut-and-paste from the original javac task before it
 * was refactored.
 *
 * @since Ant 1.3
 */
public class Javac13 extends DefaultCompilerAdapter {

    /**
     * Integer returned by the "Modern" jdk1.3 compiler to indicate success.
     */
    private static final int MODERN_COMPILER_SUCCESS = 0;

    /**
     * Run the compilation.
     * @return true if the compiler ran with a zero exit result (ok)
     * @exception BuildException if the compilation has problems.
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using modern compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupModernJavacCommand();

        // Use reflection to be able to build on all JDKs >= 1.1:
        try {
            Class<?> c = Class.forName("com.sun.tools.javac.Main");
            Object compiler = c.getDeclaredConstructor().newInstance();
            Method compile = c.getMethod("compile", String[].class);
            int result = (Integer) compile.invoke(compiler,
                    (Object) cmd.getArguments());
            return result == MODERN_COMPILER_SUCCESS;
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            }
            throw new BuildException("Error starting modern compiler",
                                     ex, location);
        }
    }
}
