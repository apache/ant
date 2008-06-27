/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.rmic;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the rmic for SUN's JDK.
 *
 * @since Ant 1.4
 */
public class SunRmic extends DefaultRmicAdapter {

    /**
     * name of the class
     */
    public static final String RMIC_CLASSNAME = "sun.rmi.rmic.Main";

    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "sun";

    /**
     * name of the executable
     */
    public static final String RMIC_EXECUTABLE = "rmic";
    /** Error message to use with the sun rmic is not the classpath. */
    public static final String ERROR_NO_RMIC_ON_CLASSPATH = "Cannot use SUN rmic, as it is not "
                                         + "available.  A common solution is to "
                                         + "set the environment variable "
                                         + "JAVA_HOME";
    /** Error message to use when there is an error starting the sun rmic compiler */
    public static final String ERROR_RMIC_FAILED = "Error starting SUN rmic: ";

    /**
     * Run the rmic compiler.
     * @return true if the compilation succeeded
     * @throws BuildException on error
     */
    public boolean execute() throws BuildException {
        getRmic().log("Using SUN rmic compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand();

        // Create an instance of the rmic, redirecting output to
        // the project log
        LogOutputStream logstr = new LogOutputStream(getRmic(),
                                                     Project.MSG_WARN);

        try {
            Class c = Class.forName(RMIC_CLASSNAME);
            Constructor cons
                = c.getConstructor(new Class[]  {OutputStream.class, String.class});
            Object rmic = cons.newInstance(new Object[] {logstr, "rmic"});

            Method doRmic = c.getMethod("compile",
                                        new Class [] {String[].class});
            Boolean ok =
                (Boolean) doRmic.invoke(rmic,
                                       (new Object[] {cmd.getArguments()}));
            return ok.booleanValue();
        } catch (ClassNotFoundException ex) {
            throw new BuildException(ERROR_NO_RMIC_ON_CLASSPATH,
                                     getRmic().getLocation());
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException(ERROR_RMIC_FAILED,
                                         ex, getRmic().getLocation());
            }
        } finally {
            try {
                logstr.close();
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }


    /**
     * Strip out all -J args from the command list.
     * @param compilerArgs the original compiler arguments
     * @return the filtered set.
     */
    protected String[] preprocessCompilerArgs(String[] compilerArgs) {
        return filterJvmCompilerArgs(compilerArgs);
    }
}
